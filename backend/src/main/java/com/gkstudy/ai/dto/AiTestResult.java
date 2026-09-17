package com.gkstudy.ai.dto;

/** 连接测试结果：状态为内部编码，statusText 为中文用户语言 */
public class AiTestResult {
    public static final String SUCCESS = "SUCCESS";
    public static final String NOT_CONFIGURED = "NOT_CONFIGURED";
    public static final String INVALID_KEY = "INVALID_KEY";
    public static final String MODEL_NOT_FOUND = "MODEL_NOT_FOUND";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
    public static final String NETWORK_ERROR = "NETWORK_ERROR";
    public static final String PROVIDER_ERROR = "PROVIDER_ERROR";

    private String provider;
    private String model;
    private String status;
    private String statusText;
    private String message;
    private long latencyMs;

    public AiTestResult() { }
    public AiTestResult(String provider, String model, String status, String message, long latencyMs) {
        this.provider = provider; this.model = model; this.status = status; this.message = message; this.latencyMs = latencyMs;
        this.statusText = statusText(status);
    }

    public static String statusText(String status) {
        if (SUCCESS.equals(status)) return "连接成功";
        if (NOT_CONFIGURED.equals(status)) return "未配置";
        if (INVALID_KEY.equals(status)) return "API Key 无效";
        if (MODEL_NOT_FOUND.equals(status)) return "模型不存在";
        if (TIMEOUT.equals(status)) return "请求超时";
        if (RATE_LIMITED.equals(status)) return "请求被限流";
        if (QUOTA_EXCEEDED.equals(status)) return "额度不足";
        if (NETWORK_ERROR.equals(status)) return "网络异常";
        return "Provider 异常";
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
}
