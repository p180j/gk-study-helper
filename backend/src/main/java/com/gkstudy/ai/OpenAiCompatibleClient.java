package com.gkstudy.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议客户端（DeepSeek / GLM / GPT / Qwen 等共用），按配置实例化，非 Spring Bean。
 */
public class OpenAiCompatibleClient implements AiProvider {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClient.class);
    private static final int MAX_RAW_LENGTH = 12000;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String providerCode;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int retryCount;

    public OpenAiCompatibleClient(RestTemplate restTemplate, ObjectMapper objectMapper, String providerCode,
                                  String baseUrl, String apiKey, String model, int retryCount) {
        this.restTemplate = restTemplate; this.objectMapper = objectMapper; this.providerCode = providerCode;
        this.baseUrl = trimSlash(baseUrl); this.apiKey = apiKey; this.model = model;
        this.retryCount = Math.max(0, Math.min(retryCount, 2));
    }

    public static RestTemplate buildRestTemplate(int timeoutSeconds) {
        SimpleRestTemplateFactory factory = new SimpleRestTemplateFactory();
        return factory.create(Math.min(timeoutSeconds, 10), timeoutSeconds);
    }

    @Override public String providerCode() { return providerCode; }
    @Override public String currentModel() { return model; }

    @Override
    public AiResponse completeStructured(String systemPrompt, String userPrompt, int maxTokens) {
        if (baseUrl.isEmpty() || apiKey == null || apiKey.trim().isEmpty() || model == null || model.trim().isEmpty()) {
            throw new AiProviderException("AI_NOT_CONFIGURED", "AI 服务尚未配置，请在管理控台配置 AI Provider");
        }
        LocalDateTime requestTime = LocalDateTime.now();
        long started = System.nanoTime();
        RuntimeException last = null;
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                String raw = request(systemPrompt, userPrompt, maxTokens);
                JsonNode content = parseContent(raw);
                return new AiResponse(providerCode, model, content, limit(raw), requestTime, elapsed(started));
            } catch (AiProviderException e) {
                throw e;
            } catch (RestClientException e) {
                last = classify(e);
                log.warn("AI request failed: provider={}, attempt={}, reason={}", providerCode, attempt + 1, e.getClass().getSimpleName());
            } catch (Exception e) {
                throw new AiProviderException("AI_RESPONSE_INVALID", "AI 返回内容无法解析", e);
            }
        }
        throw last;
    }

    static AiProviderException classify(RestClientException e) {
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException http = (HttpClientErrorException) e;
            String body = http.getResponseBodyAsString() == null ? "" : http.getResponseBodyAsString().toLowerCase();
            int status = http.getRawStatusCode();
            if (status == 401 || status == 403) return new AiProviderException("AI_INVALID_KEY", "API Key 无效或无权限", e);
            if (status == 404) return new AiProviderException("AI_MODEL_NOT_FOUND", "模型或接口不存在", e);
            if (status == 429) {
                if (body.contains("quota") || body.contains("balance") || body.contains("insufficient")
                        || body.contains("额度") || body.contains("余额")) {
                    return new AiProviderException("AI_QUOTA_EXCEEDED", "账户额度不足", e);
                }
                return new AiProviderException("AI_RATE_LIMITED", "请求被限流，请稍后重试", e);
            }
            if (status == 400 && (body.contains("model") && (body.contains("not found") || body.contains("does not exist")))) {
                return new AiProviderException("AI_MODEL_NOT_FOUND", "模型不存在", e);
            }
            return new AiProviderException("AI_HTTP_ERROR", "AI 服务返回错误（HTTP " + status + "）", e);
        }
        if (e instanceof HttpServerErrorException) {
            return new AiProviderException("AI_HTTP_ERROR", "AI 服务暂时不可用（HTTP "
                    + ((HttpServerErrorException) e).getRawStatusCode() + "）", e);
        }
        if (e instanceof ResourceAccessException) {
            Throwable cause = e.getCause();
            if (cause instanceof SocketTimeoutException) return new AiProviderException("AI_TIMEOUT", "请求超时", e);
            if (cause instanceof UnknownHostException) return new AiProviderException("AI_NETWORK_ERROR", "网络异常，无法连接 AI 服务", e);
            return new AiProviderException("AI_NETWORK_ERROR", "网络异常，无法连接 AI 服务", e);
        }
        return new AiProviderException("AI_HTTP_ERROR", "AI 服务请求失败", e);
    }

    @Override
    public boolean healthCheck() {
        try {
            restTemplate.exchange(baseUrl + "/models", HttpMethod.GET, new HttpEntity<>(headers()), String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String request(String systemPrompt, String userPrompt, int maxTokens) throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt)); messages.add(message("user", userPrompt));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model); body.put("messages", messages); body.put("temperature", 0.1);
        body.put("max_tokens", Math.max(256, maxTokens));
        Map<String, String> responseFormat = new LinkedHashMap<>();
        responseFormat.put("type", "json_object"); body.put("response_format", responseFormat);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + "/chat/completions",
                new HttpEntity<>(body, headers()), String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().trim().isEmpty()) {
            throw new AiProviderException("AI_EMPTY_RESULT", "AI 服务未返回有效内容");
        }
        return response.getBody();
    }

    private JsonNode parseContent(String raw) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (!content.isTextual() || content.asText().trim().isEmpty()) {
            throw new AiProviderException("AI_EMPTY_RESULT", "AI 模型拒绝或未返回结构化结果");
        }
        String text = content.asText().trim();
        if (text.startsWith("```")) text = text.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        try { return objectMapper.readTree(text); }
        catch (Exception e) { throw new AiProviderException("AI_JSON_INVALID", "AI 返回的 JSON 不合法", e); }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON); headers.setBearerAuth(apiKey); return headers;
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> result = new LinkedHashMap<>(); result.put("role", role); result.put("content", content); return result;
    }

    private String trimSlash(String value) {
        if (value == null) return "";
        String result = value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private String limit(String value) { return value.length() <= MAX_RAW_LENGTH ? value : value.substring(0, MAX_RAW_LENGTH); }
    private long elapsed(long started) { return (System.nanoTime() - started) / 1_000_000L; }

    /** 独立的 RestTemplate 构造器，避免依赖 Spring Boot Builder（便于脱离容器实例化） */
    static class SimpleRestTemplateFactory {
        RestTemplate create(int connectTimeoutSeconds, int readTimeoutSeconds) {
            org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                    new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout((int) Duration.ofSeconds(connectTimeoutSeconds).toMillis());
            factory.setReadTimeout((int) Duration.ofSeconds(readTimeoutSeconds).toMillis());
            return new RestTemplate(factory);
        }
    }
}
