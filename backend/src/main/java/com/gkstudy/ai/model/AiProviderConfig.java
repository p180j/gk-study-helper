package com.gkstudy.ai.model;

import java.time.LocalDateTime;

public class AiProviderConfig {
    private Long id;
    private String providerCode;
    private boolean enabled;
    private String model;
    private String customModel;
    private String baseUrl;
    /** AES-GCM 密文，永不返回给前端 */
    private String apiKeyCipher;
    private String maskedKey;
    private boolean defaultProvider;
    private String lastTestStatus;
    private String lastTestMessage;
    private LocalDateTime lastTestTime;
    private Integer lastTestLatencyMs;
    private String lastTestModel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getCustomModel() { return customModel; }
    public void setCustomModel(String customModel) { this.customModel = customModel; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKeyCipher() { return apiKeyCipher; }
    public void setApiKeyCipher(String apiKeyCipher) { this.apiKeyCipher = apiKeyCipher; }
    public String getMaskedKey() { return maskedKey; }
    public void setMaskedKey(String maskedKey) { this.maskedKey = maskedKey; }
    public boolean isDefaultProvider() { return defaultProvider; }
    public void setDefaultProvider(boolean defaultProvider) { this.defaultProvider = defaultProvider; }
    public String getLastTestStatus() { return lastTestStatus; }
    public void setLastTestStatus(String lastTestStatus) { this.lastTestStatus = lastTestStatus; }
    public String getLastTestMessage() { return lastTestMessage; }
    public void setLastTestMessage(String lastTestMessage) { this.lastTestMessage = lastTestMessage; }
    public LocalDateTime getLastTestTime() { return lastTestTime; }
    public void setLastTestTime(LocalDateTime lastTestTime) { this.lastTestTime = lastTestTime; }
    public Integer getLastTestLatencyMs() { return lastTestLatencyMs; }
    public void setLastTestLatencyMs(Integer lastTestLatencyMs) { this.lastTestLatencyMs = lastTestLatencyMs; }
    public String getLastTestModel() { return lastTestModel; }
    public void setLastTestModel(String lastTestModel) { this.lastTestModel = lastTestModel; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    /** 生效模型：自定义模型优先 */
    public String effectiveModel() {
        return customModel != null && !customModel.trim().isEmpty() ? customModel.trim() : model;
    }

    /** 生效 Base URL：覆盖值优先，其次默认 */
    public String effectiveBaseUrl() {
        return baseUrl != null && !baseUrl.trim().isEmpty() ? baseUrl.trim() : AiProviderDefaultsHolder.defaultBaseUrl(providerCode);
    }

    /** 是否已配置可用 Key */
    public boolean hasKey() { return apiKeyCipher != null && !apiKeyCipher.trim().isEmpty(); }

    private static final class AiProviderDefaultsHolder {
        static String defaultBaseUrl(String code) { return com.gkstudy.ai.AiProviderDefaults.defaultBaseUrl(code); }
    }
}
