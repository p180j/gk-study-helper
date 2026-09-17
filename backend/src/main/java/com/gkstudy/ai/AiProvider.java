package com.gkstudy.ai;

public interface AiProvider {
    AiResponse completeStructured(String systemPrompt, String userPrompt, int maxTokens);
    boolean healthCheck();

    /** Provider 内部编码（DEEPSEEK/GEMINI/GLM/OPENAI/QWEN），仅用于审计与配置路由，禁止业务模块做厂商判断 */
    default String providerCode() { return "OPENAI_COMPATIBLE"; }

    /** 当前生效模型名（用于展示与审计） */
    default String currentModel() { return ""; }
}
