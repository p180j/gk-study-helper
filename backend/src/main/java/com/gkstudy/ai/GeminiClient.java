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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini 客户端（generateContent 协议），按配置实例化，非 Spring Bean。
 */
public class GeminiClient implements AiProvider {
    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final int MAX_RAW_LENGTH = 12000;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int retryCount;

    public GeminiClient(RestTemplate restTemplate, ObjectMapper objectMapper, String baseUrl, String apiKey, String model, int retryCount) {
        this.restTemplate = restTemplate; this.objectMapper = objectMapper;
        this.baseUrl = trimSlash(baseUrl); this.apiKey = apiKey; this.model = model;
        this.retryCount = Math.max(0, Math.min(retryCount, 2));
    }

    @Override public String providerCode() { return "GEMINI"; }
    @Override public String currentModel() { return model; }

    @Override
    public AiResponse completeStructured(String systemPrompt, String userPrompt, int maxTokens) {
        LocalDateTime requestTime = LocalDateTime.now();
        long started = System.nanoTime();
        RuntimeException last = null;
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                String raw = request(systemPrompt, userPrompt, maxTokens);
                JsonNode content = parseContent(raw);
                return new AiResponse("GEMINI", model, content, limit(raw), requestTime, elapsed(started));
            } catch (AiProviderException e) {
                throw e;
            } catch (RestClientException e) {
                last = classify(e);
                log.warn("Gemini request failed: attempt={}, model={}, reason={}", attempt + 1, model, e.getClass().getSimpleName());
            } catch (Exception e) {
                throw new AiProviderException("AI_RESPONSE_INVALID", "AI 返回内容无法解析", e);
            }
        }
        throw last;
    }

    private AiProviderException classify(RestClientException e) {
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException http = (HttpClientErrorException) e;
            String body = http.getResponseBodyAsString() == null ? "" : http.getResponseBodyAsString().toLowerCase();
            int status = http.getRawStatusCode();
            if (status == 400 && (body.contains("api key not valid") || body.contains("api_key_invalid"))) {
                return new AiProviderException("AI_INVALID_KEY", "API Key 无效", e);
            }
            if (status == 401 || status == 403) return new AiProviderException("AI_INVALID_KEY", "API Key 无效或无权限", e);
            if (status == 404) return new AiProviderException("AI_MODEL_NOT_FOUND", "模型不存在", e);
            if (status == 429) {
                if (body.contains("quota")) return new AiProviderException("AI_QUOTA_EXCEEDED", "账户额度不足", e);
                return new AiProviderException("AI_RATE_LIMITED", "请求被限流，请稍后重试", e);
            }
            if (status == 400 && body.contains("not found")) return new AiProviderException("AI_MODEL_NOT_FOUND", "模型不存在", e);
            return new AiProviderException("AI_HTTP_ERROR", "AI 服务返回错误（HTTP " + status + "）", e);
        }
        if (e instanceof HttpServerErrorException) {
            return new AiProviderException("AI_HTTP_ERROR", "AI 服务暂时不可用（HTTP "
                    + ((HttpServerErrorException) e).getRawStatusCode() + "）", e);
        }
        return OpenAiCompatibleClient.classify(e);
    }

    @Override
    public boolean healthCheck() {
        try {
            restTemplate.exchange(baseUrl + "/v1beta/models", HttpMethod.GET, new HttpEntity<>(headers()), String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String request(String systemPrompt, String userPrompt, int maxTokens) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> systemPart = new LinkedHashMap<>();
        List<Map<String, Object>> systemParts = new ArrayList<>();
        systemPart.put("text", systemPrompt); systemParts.add(systemPart);
        body.put("system_instruction", Collections.singletonMap("parts", systemParts));
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", "user");
        content.put("parts", Collections.singletonList(Collections.singletonMap("text", userPrompt)));
        contents.add(content); body.put("contents", contents);
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("maxOutputTokens", Math.max(256, maxTokens));
        generationConfig.put("responseMimeType", "application/json");
        body.put("generationConfig", generationConfig);
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/v1beta/models/" + model + ":generateContent", new HttpEntity<>(body, headers()), String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().trim().isEmpty()) {
            throw new AiProviderException("AI_EMPTY_RESULT", "AI 服务未返回有效内容");
        }
        return response.getBody();
    }

    private JsonNode parseContent(String raw) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray() || parts.size() == 0) {
            throw new AiProviderException("AI_EMPTY_RESULT", "AI 模型拒绝或未返回结构化结果");
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) if (part.path("text").isTextual()) text.append(part.get("text").asText());
        String value = text.toString().trim();
        if (value.isEmpty()) throw new AiProviderException("AI_EMPTY_RESULT", "AI 模型未返回内容");
        if (value.startsWith("```")) value = value.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        try { return objectMapper.readTree(value); }
        catch (Exception e) { throw new AiProviderException("AI_JSON_INVALID", "AI 返回的 JSON 不合法", e); }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey); return headers;
    }

    private String trimSlash(String value) {
        if (value == null) return "";
        String result = value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private String limit(String value) { return value.length() <= MAX_RAW_LENGTH ? value : value.substring(0, MAX_RAW_LENGTH); }
    private long elapsed(long started) { return (System.nanoTime() - started) / 1_000_000L; }
}
