package com.gkstudy.ai;

import com.gkstudy.ai.mapper.AiProviderConfigMapper;
import com.gkstudy.ai.model.AiProviderConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Provider 注册中心：按数据库配置动态构建当前生效的 AI 客户端。
 * 业务模块只依赖 AiService/AiProvider，禁止在此之外判断具体厂商。
 * 数据库未配置默认 Provider 时，回退到环境变量（AI_BASE_URL / AI_API_KEY / AI_MODEL，兼容第八阶段部署）。
 */
@Component
public class ProviderRegistry {
    private static final Logger log = LoggerFactory.getLogger(ProviderRegistry.class);
    private final AiProviderConfigMapper configMapper;
    private final ApiKeyCipher apiKeyCipher;
    private final ObjectMapper objectMapper;
    private final String legacyBaseUrl;
    private final String legacyApiKey;
    private final String legacyModel;
    private final int timeoutSeconds;
    private final int retryCount;

    /** 客户端缓存：配置变更（updateTime 变化）后自动重建 */
    private volatile AiProvider active;
    private volatile String activeSignature;

    public ProviderRegistry(AiProviderConfigMapper configMapper, ApiKeyCipher apiKeyCipher, ObjectMapper objectMapper,
                            @Value("${ai.base-url:}") String legacyBaseUrl,
                            @Value("${ai.api-key:}") String legacyApiKey,
                            @Value("${ai.model:}") String legacyModel,
                            @Value("${ai.timeout-seconds:45}") int timeoutSeconds,
                            @Value("${ai.retry-count:1}") int retryCount) {
        this.configMapper = configMapper; this.apiKeyCipher = apiKeyCipher; this.objectMapper = objectMapper;
        this.legacyBaseUrl = legacyBaseUrl; this.legacyApiKey = legacyApiKey; this.legacyModel = legacyModel;
        this.timeoutSeconds = timeoutSeconds; this.retryCount = retryCount;
    }

    /** 当前生效 Provider：数据库默认启用配置优先，其次环境变量回退 */
    public AiProvider active() {
        AiProviderConfig config = defaultConfig();
        String signature = signature(config);
        AiProvider current = active;
        if (current != null && signature.equals(activeSignature)) return current;
        synchronized (this) {
            if (active != null && signature.equals(activeSignature)) return active;
            AiProvider provider = build(config);
            this.active = provider; this.activeSignature = signature;
            return provider;
        }
    }

    /** 按指定配置构建客户端（测试连接用） */
    public AiProvider clientFor(AiProviderConfig config) {
        return build(config);
    }

    /** 按明文参数构建客户端（管理端保存前测试连接用），厂商协议判断只允许出现在注册中心 */
    public AiProvider clientFor(String code, String baseUrl, String apiKey, String model) {
        if (AiProviderDefaults.GEMINI.equals(code)) {
            return new GeminiClient(OpenAiCompatibleClient.buildRestTemplate(timeoutSeconds), objectMapper,
                    baseUrl, apiKey, model, retryCount);
        }
        return new OpenAiCompatibleClient(OpenAiCompatibleClient.buildRestTemplate(timeoutSeconds), objectMapper,
                code, baseUrl, apiKey, model, retryCount);
    }

    private AiProviderConfig defaultConfig() {
        try {
            List<AiProviderConfig> defaults = configMapper.findDefaultEnabled();
            if (!defaults.isEmpty()) {
                AiProviderConfig config = defaults.get(0);
                if (config.hasKey() && config.effectiveModel() != null && !config.effectiveModel().isEmpty()) return config;
            }
        } catch (Exception e) {
            log.warn("读取 AI Provider 配置失败，回退环境变量配置: {}", e.getMessage());
        }
        return null;
    }

    private AiProvider build(AiProviderConfig config) {
        if (config == null) return legacyClient();
        try {
            String apiKey = apiKeyCipher.decrypt(config.getApiKeyCipher());
            return clientFor(config.getProviderCode(), config.effectiveBaseUrl(), apiKey, config.effectiveModel());
        } catch (Exception e) {
            log.warn("AI Provider 配置解密失败，回退环境变量配置: {}", e.getMessage());
            return legacyClient();
        }
    }

    private AiProvider legacyClient() {
        return new OpenAiCompatibleClient(OpenAiCompatibleClient.buildRestTemplate(timeoutSeconds), objectMapper,
                "OPENAI_COMPATIBLE", legacyBaseUrl, legacyApiKey, legacyModel, retryCount);
    }

    private String signature(AiProviderConfig config) {
        if (config == null) return "legacy:" + legacyBaseUrl + ":" + legacyModel + ":" + (legacyApiKey == null ? "" : legacyApiKey.hashCode());
        return config.getProviderCode() + ":" + config.effectiveModel() + ":" + config.getBaseUrl() + ":"
                + config.getUpdateTime() + ":" + config.getApiKeyCipher().hashCode();
    }
}
