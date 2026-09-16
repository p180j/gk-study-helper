package com.gkstudy.ai;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public class AiResponse {
    private final String provider;
    private final String model;
    private final JsonNode content;
    private final String rawResponse;
    private final LocalDateTime requestTime;
    private final long latencyMs;

    public AiResponse(String provider, String model, JsonNode content, String rawResponse,
                      LocalDateTime requestTime, long latencyMs) {
        this.provider = provider; this.model = model; this.content = content; this.rawResponse = rawResponse;
        this.requestTime = requestTime; this.latencyMs = latencyMs;
    }

    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public JsonNode getContent() { return content; }
    public String getRawResponse() { return rawResponse; }
    public LocalDateTime getRequestTime() { return requestTime; }
    public long getLatencyMs() { return latencyMs; }
}
