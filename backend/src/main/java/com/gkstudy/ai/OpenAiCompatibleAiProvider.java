package com.gkstudy.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleAiProvider implements AiProvider {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleAiProvider.class);
    private static final int MAX_RAW_LENGTH = 12000;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int retryCount;

    public OpenAiCompatibleAiProvider(RestTemplateBuilder builder, ObjectMapper objectMapper,
                                      @Value("${ai.base-url:}") String baseUrl,
                                      @Value("${ai.api-key:}") String apiKey,
                                      @Value("${ai.model:}") String model,
                                      @Value("${ai.timeout-seconds:45}") int timeoutSeconds,
                                      @Value("${ai.retry-count:1}") int retryCount) {
        this.restTemplate = builder.setConnectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 10)))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds)).build();
        this.objectMapper = objectMapper; this.baseUrl = trimSlash(baseUrl); this.apiKey = apiKey;
        this.model = model; this.retryCount = Math.max(0, Math.min(retryCount, 2));
    }

    @Override
    public AiResponse completeStructured(String systemPrompt, String userPrompt, int maxTokens) {
        requireConfigured();
        LocalDateTime requestTime = LocalDateTime.now();
        long started = System.nanoTime();
        RuntimeException last = null;
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                String raw = request(systemPrompt, userPrompt, maxTokens);
                JsonNode content = parseContent(raw);
                return new AiResponse("OPENAI_COMPATIBLE", model, content, limit(raw), requestTime, elapsed(started));
            } catch (RestClientException e) {
                last = new AiProviderException("AI_HTTP_ERROR", "AI 服务请求失败", e);
                log.warn("AI request failed: attempt={}, model={}, reason={}", attempt + 1, model, e.getClass().getSimpleName());
            } catch (AiProviderException e) {
                throw e;
            } catch (Exception e) {
                throw new AiProviderException("AI_RESPONSE_INVALID", "AI 返回内容无法解析", e);
            }
        }
        throw last;
    }

    @Override
    public boolean healthCheck() {
        if (!configured()) return false;
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
        Map<String, String> thinking = new LinkedHashMap<>();
        thinking.put("type", "disabled"); body.put("thinking", thinking);
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

    private void requireConfigured() {
        if (!configured()) throw new AiProviderException("AI_NOT_CONFIGURED", "AI 服务尚未配置，可稍后重试");
    }

    private boolean configured() {
        return !baseUrl.isEmpty() && apiKey != null && !apiKey.trim().isEmpty() && model != null && !model.trim().isEmpty();
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
