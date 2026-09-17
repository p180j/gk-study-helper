package com.gkstudy.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 管理端 AI Provider 展示视图：永不包含明文或密文 API Key */
public class AiProviderView {
    private String code;
    private String name;
    private boolean enabled;
    private boolean configured;
    private boolean defaultProvider;
    private String model;
    private String customModel;
    private String effectiveModel;
    private String baseUrl;
    private String defaultBaseUrl;
    private String maskedKey;
    private List<String> defaultModels;
    private String lastTestStatus;
    private String lastTestStatusText;
    private String lastTestMessage;
    private LocalDateTime lastTestTime;
    private Integer lastTestLatencyMs;
    private String lastTestModel;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }
    public boolean isDefaultProvider() { return defaultProvider; }
    public void setDefaultProvider(boolean defaultProvider) { this.defaultProvider = defaultProvider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getCustomModel() { return customModel; }
    public void setCustomModel(String customModel) { this.customModel = customModel; }
    public String getEffectiveModel() { return effectiveModel; }
    public void setEffectiveModel(String effectiveModel) { this.effectiveModel = effectiveModel; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getDefaultBaseUrl() { return defaultBaseUrl; }
    public void setDefaultBaseUrl(String defaultBaseUrl) { this.defaultBaseUrl = defaultBaseUrl; }
    public String getMaskedKey() { return maskedKey; }
    public void setMaskedKey(String maskedKey) { this.maskedKey = maskedKey; }
    public List<String> getDefaultModels() { return defaultModels; }
    public void setDefaultModels(List<String> defaultModels) { this.defaultModels = defaultModels; }
    public String getLastTestStatus() { return lastTestStatus; }
    public void setLastTestStatus(String lastTestStatus) { this.lastTestStatus = lastTestStatus; }
    public String getLastTestStatusText() { return lastTestStatusText; }
    public void setLastTestStatusText(String lastTestStatusText) { this.lastTestStatusText = lastTestStatusText; }
    public String getLastTestMessage() { return lastTestMessage; }
    public void setLastTestMessage(String lastTestMessage) { this.lastTestMessage = lastTestMessage; }
    public LocalDateTime getLastTestTime() { return lastTestTime; }
    public void setLastTestTime(LocalDateTime lastTestTime) { this.lastTestTime = lastTestTime; }
    public Integer getLastTestLatencyMs() { return lastTestLatencyMs; }
    public void setLastTestLatencyMs(Integer lastTestLatencyMs) { this.lastTestLatencyMs = lastTestLatencyMs; }
    public String getLastTestModel() { return lastTestModel; }
    public void setLastTestModel(String lastTestModel) { this.lastTestModel = lastTestModel; }
}
