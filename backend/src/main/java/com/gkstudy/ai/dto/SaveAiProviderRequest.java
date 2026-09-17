package com.gkstudy.ai.dto;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class SaveAiProviderRequest {
    /** 仅修改 Key 时传入完整明文；不修改则留空。禁止传入 maskedKey */
    @Size(max = 512, message = "API Key 过长")
    private String apiKey;
    @Size(max = 100, message = "模型名过长")
    private String model;
    @Size(max = 100, message = "自定义模型名过长")
    private String customModel;
    @Size(max = 255, message = "Base URL 过长")
    private String baseUrl;
    @Pattern(regexp = "true|false", message = "启用状态不合法")
    private String enabled;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getCustomModel() { return customModel; }
    public void setCustomModel(String customModel) { this.customModel = customModel; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
}
