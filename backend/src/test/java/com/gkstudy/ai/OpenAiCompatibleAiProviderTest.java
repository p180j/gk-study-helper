package com.gkstudy.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiCompatibleAiProviderTest {
    private HttpServer server;

    @AfterEach void stop() { if (server != null) server.stop(0); }

    @Test
    void parsesStructuredJsonFromOpenAiCompatibleResponse() throws Exception {
        String body = "{\"choices\":[{\"message\":{\"content\":\"{\\\"score\\\":88}\"}}]}";
        OpenAiCompatibleAiProvider provider = provider(body);
        assertEquals(88, provider.completeStructured("system", "user", 256).getContent().get("score").asInt());
    }

    @Test
    void rejectsInvalidJsonContent() throws Exception {
        OpenAiCompatibleAiProvider provider = provider("{\"choices\":[{\"message\":{\"content\":\"not-json\"}}]}");
        AiProviderException error = assertThrows(AiProviderException.class,
                () -> provider.completeStructured("system", "user", 256));
        assertEquals("AI_JSON_INVALID", error.getCode());
    }

    private OpenAiCompatibleAiProvider provider(String responseBody) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        return new OpenAiCompatibleAiProvider(new RestTemplateBuilder(), new ObjectMapper(),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "test-key", "test-model", 5, 0);
    }
}
