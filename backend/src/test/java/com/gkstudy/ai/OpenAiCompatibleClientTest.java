package com.gkstudy.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiCompatibleClientTest {
    private HttpServer server;

    @AfterEach void stop() { if (server != null) server.stop(0); }

    @Test
    void parsesStructuredJsonFromOpenAiCompatibleResponse() throws Exception {
        String body = "{\"choices\":[{\"message\":{\"content\":\"{\\\"score\\\":88}\"}}]}";
        OpenAiCompatibleClient provider = client(body, 200);
        AiResponse response = provider.completeStructured("system", "user", 256);
        assertEquals(88, response.getContent().get("score").asInt());
        assertEquals("DEEPSEEK", response.getProvider());
        assertEquals("test-model", response.getModel());
    }

    @Test
    void rejectsInvalidJsonContent() throws Exception {
        OpenAiCompatibleClient provider = client("{\"choices\":[{\"message\":{\"content\":\"not-json\"}}]}", 200);
        AiProviderException error = assertThrows(AiProviderException.class,
                () -> provider.completeStructured("system", "user", 256));
        assertEquals("AI_JSON_INVALID", error.getCode());
    }

    @Test
    void classifiesInvalidKeyFromUnauthorized() throws Exception {
        OpenAiCompatibleClient provider = client("{\"error\":\"invalid api key\"}", 401);
        AiProviderException error = assertThrows(AiProviderException.class,
                () -> provider.completeStructured("system", "user", 256));
        assertEquals("AI_INVALID_KEY", error.getCode());
    }

    @Test
    void classifiesRateLimitFromTooManyRequests() throws Exception {
        OpenAiCompatibleClient provider = client("{\"error\":\"rate limited\"}", 429);
        AiProviderException error = assertThrows(AiProviderException.class,
                () -> provider.completeStructured("system", "user", 256));
        assertEquals("AI_RATE_LIMITED", error.getCode());
    }

    @Test
    void classifiesQuotaExceededFromInsufficientBalance() throws Exception {
        OpenAiCompatibleClient provider = client("{\"error\":\"insufficient balance\"}", 429);
        AiProviderException error = assertThrows(AiProviderException.class,
                () -> provider.completeStructured("system", "user", 256));
        assertEquals("AI_QUOTA_EXCEEDED", error.getCode());
    }

    @Test
    void classifiesModelNotFoundFrom404() throws Exception {
        OpenAiCompatibleClient provider = client("{\"error\":\"not found\"}", 404);
        AiProviderException error = assertThrows(AiProviderException.class,
                () -> provider.completeStructured("system", "user", 256));
        assertEquals("AI_MODEL_NOT_FOUND", error.getCode());
    }

    @Test
    void rejectsWhenNotConfigured() {
        OpenAiCompatibleClient provider = new OpenAiCompatibleClient(
                OpenAiCompatibleClient.buildRestTemplate(5), new ObjectMapper(), "DEEPSEEK", "", "", "", 0);
        AiProviderException error = assertThrows(AiProviderException.class,
                () -> provider.completeStructured("system", "user", 256));
        assertEquals("AI_NOT_CONFIGURED", error.getCode());
    }

    private OpenAiCompatibleClient client(String responseBody, int status) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
            exchange.close();
        });
        server.start();
        return new OpenAiCompatibleClient(OpenAiCompatibleClient.buildRestTemplate(5), new ObjectMapper(), "DEEPSEEK",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "test-key", "test-model", 0);
    }
}
