package com.gkstudy.ai;

public interface AiProvider {
    AiResponse completeStructured(String systemPrompt, String userPrompt, int maxTokens);
    boolean healthCheck();
}
