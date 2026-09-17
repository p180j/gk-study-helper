package com.gkstudy.ai;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 五家 Provider 的内置默认信息：显示名、默认 Base URL、默认模型列表。
 * 模型不写死单一值，用户可自定义模型名以适应版本变化。
 */
public final class AiProviderDefaults {
    public static final String DEEPSEEK = "DEEPSEEK";
    public static final String GEMINI = "GEMINI";
    public static final String GLM = "GLM";
    public static final String OPENAI = "OPENAI";
    public static final String QWEN = "QWEN";

    private static final Map<String, ProviderInfo> PROVIDERS = new LinkedHashMap<>();
    static {
        PROVIDERS.put(DEEPSEEK, new ProviderInfo(DEEPSEEK, "DeepSeek", "https://api.deepseek.com",
                Arrays.asList("deepseek-chat", "deepseek-reasoner"), true));
        PROVIDERS.put(GEMINI, new ProviderInfo(GEMINI, "Gemini", "https://generativelanguage.googleapis.com",
                Arrays.asList("gemini-2.0-flash", "gemini-1.5-flash"), false));
        PROVIDERS.put(GLM, new ProviderInfo(GLM, "GLM / 智谱", "https://open.bigmodel.cn/api/paas/v4",
                Arrays.asList("glm-4-flash", "glm-4-air", "glm-4-plus"), true));
        PROVIDERS.put(OPENAI, new ProviderInfo(OPENAI, "GPT / OpenAI", "https://api.openai.com/v1",
                Arrays.asList("gpt-4o-mini", "gpt-4o"), true));
        PROVIDERS.put(QWEN, new ProviderInfo(QWEN, "Qwen / 通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1",
                Arrays.asList("qwen-plus", "qwen-turbo", "qwen-max"), true));
    }

    private AiProviderDefaults() { }

    public static List<String> codes() { return Collections.unmodifiableList(new java.util.ArrayList<>(PROVIDERS.keySet())); }

    public static boolean supported(String code) { return PROVIDERS.containsKey(code); }

    public static String displayName(String code) {
        ProviderInfo info = PROVIDERS.get(code);
        return info == null ? code : info.displayName;
    }

    public static String defaultBaseUrl(String code) {
        ProviderInfo info = PROVIDERS.get(code);
        return info == null ? "" : info.baseUrl;
    }

    public static List<String> defaultModels(String code) {
        ProviderInfo info = PROVIDERS.get(code);
        return info == null ? Collections.emptyList() : Collections.unmodifiableList(info.models);
    }

    public static String defaultModel(String code) {
        List<String> models = defaultModels(code);
        return models.isEmpty() ? "" : models.get(0);
    }

    /** 是否走 OpenAI 兼容协议（否则走 Gemini generateContent 协议） */
    public static boolean openAiCompatible(String code) {
        ProviderInfo info = PROVIDERS.get(code);
        return info == null || info.openAiCompatible;
    }

    public static final class ProviderInfo {
        final String code; final String displayName; final String baseUrl; final List<String> models; final boolean openAiCompatible;
        ProviderInfo(String code, String displayName, String baseUrl, List<String> models, boolean openAiCompatible) {
            this.code = code; this.displayName = displayName; this.baseUrl = baseUrl; this.models = models; this.openAiCompatible = openAiCompatible;
        }
    }
}
