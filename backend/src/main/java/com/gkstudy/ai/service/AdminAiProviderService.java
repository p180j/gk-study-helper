package com.gkstudy.ai.service;

import com.gkstudy.ai.AiProvider;
import com.gkstudy.ai.AiProviderDefaults;
import com.gkstudy.ai.AiProviderException;
import com.gkstudy.ai.ApiKeyCipher;
import com.gkstudy.ai.ProviderRegistry;
import com.gkstudy.ai.dto.AiProviderView;
import com.gkstudy.ai.dto.AiTestResult;
import com.gkstudy.ai.dto.SaveAiProviderRequest;
import com.gkstudy.ai.mapper.AiProviderConfigMapper;
import com.gkstudy.ai.model.AiProviderConfig;
import com.gkstudy.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminAiProviderService {
    private static final Logger log = LoggerFactory.getLogger(AdminAiProviderService.class);
    private final AiProviderConfigMapper configMapper;
    private final ApiKeyCipher apiKeyCipher;
    private final ProviderRegistry registry;

    public AdminAiProviderService(AiProviderConfigMapper configMapper, ApiKeyCipher apiKeyCipher, ProviderRegistry registry) {
        this.configMapper = configMapper; this.apiKeyCipher = apiKeyCipher; this.registry = registry;
    }

    public List<AiProviderView> list() {
        List<AiProviderView> views = new ArrayList<>();
        for (String code : AiProviderDefaults.codes()) {
            AiProviderConfig config = configMapper.findByCode(code);
            views.add(view(code, config));
        }
        return views;
    }

    @Transactional
    public AiProviderView save(String code, SaveAiProviderRequest request) {
        if (!AiProviderDefaults.supported(code)) throw new BusinessException("AI_PROVIDER_NOT_FOUND", "不支持的 AI Provider");
        AiProviderConfig config = configMapper.findByCode(code);
        boolean create = config == null;
        if (create) {
            config = new AiProviderConfig();
            config.setProviderCode(code);
            config.setModel(AiProviderDefaults.defaultModel(code));
            config.setEnabled(false);
            config.setDefaultProvider(false);
        }
        if (request.getModel() != null && !request.getModel().trim().isEmpty()) config.setModel(request.getModel().trim());
        config.setCustomModel(trimToNull(request.getCustomModel()));
        config.setBaseUrl(trimToNull(request.getBaseUrl()));
        boolean enabled = request.getEnabled() == null ? config.isEnabled() : Boolean.parseBoolean(request.getEnabled());
        config.setEnabled(enabled);
        if (request.getApiKey() != null && !request.getApiKey().trim().isEmpty()) {
            String plaintext = request.getApiKey().trim();
            if (plaintext.contains("****")) {
                throw new BusinessException("AI_KEY_INVALID", "修改 API Key 时必须重新输入完整 Key，不能使用掩码值");
            }
            if (!apiKeyCipher.masterKeyConfigured()) {
                throw new BusinessException("AI_MASTER_KEY_MISSING",
                        "服务器未配置环境变量 " + ApiKeyCipher.MASTER_KEY_ENV + "，无法保存 API Key");
            }
            config.setApiKeyCipher(apiKeyCipher.encrypt(plaintext));
            config.setMaskedKey(apiKeyCipher.mask(plaintext));
        }
        if (create) configMapper.insert(config); else configMapper.update(config);
        return view(code, configMapper.findByCode(code));
    }

    public AiTestResult test(String code) {
        if (!AiProviderDefaults.supported(code)) throw new BusinessException("AI_PROVIDER_NOT_FOUND", "不支持的 AI Provider");
        AiProviderConfig config = configMapper.findByCode(code);
        AiTestResult result = doTest(code, config, null);
        persistTestResult(code, result);
        return result;
    }

    /** 保存后立即测试：优先使用请求中的新 Key（未落库前直接测试） */
    public AiTestResult testWithRequest(String code, SaveAiProviderRequest request) {
        if (!AiProviderDefaults.supported(code)) throw new BusinessException("AI_PROVIDER_NOT_FOUND", "不支持的 AI Provider");
        AiProviderConfig merged = mergeForTest(code, request);
        AiTestResult result = doTest(code, merged, request.getApiKey());
        persistTestResult(code, result);
        return result;
    }

    @Transactional
    public AiProviderView setDefault(String code) {
        if (!AiProviderDefaults.supported(code)) throw new BusinessException("AI_PROVIDER_NOT_FOUND", "不支持的 AI Provider");
        AiProviderConfig config = configMapper.findByCode(code);
        if (config == null || !config.hasKey()) throw new BusinessException("AI_PROVIDER_NOT_CONFIGURED", "请先配置该 Provider 的 API Key");
        if (!config.isEnabled()) throw new BusinessException("AI_PROVIDER_NOT_ENABLED", "请先启用该 Provider");
        configMapper.switchDefault(code);
        return view(code, configMapper.findByCode(code));
    }

    private AiProviderConfig mergeForTest(String code, SaveAiProviderRequest request) {
        AiProviderConfig config = configMapper.findByCode(code);
        if (config == null) {
            config = new AiProviderConfig();
            config.setProviderCode(code);
            config.setModel(AiProviderDefaults.defaultModel(code));
        }
        if (request.getModel() != null && !request.getModel().trim().isEmpty()) config.setModel(request.getModel().trim());
        if (request.getCustomModel() != null) config.setCustomModel(trimToNull(request.getCustomModel()));
        if (request.getBaseUrl() != null) config.setBaseUrl(trimToNull(request.getBaseUrl()));
        return config;
    }

    private AiTestResult doTest(String code, AiProviderConfig config, String plaintextKey) {
        String model = config == null ? "" : config.effectiveModel();
        if (config == null || !config.hasKey() && (plaintextKey == null || plaintextKey.trim().isEmpty())) {
            return new AiTestResult(code, model, AiTestResult.NOT_CONFIGURED, "尚未配置 API Key", 0);
        }
        String apiKey = plaintextKey != null && !plaintextKey.trim().isEmpty()
                ? plaintextKey.trim() : apiKeyCipher.decrypt(config.getApiKeyCipher());
        try {
            AiProvider client = registry.clientFor(code, config.effectiveBaseUrl(), apiKey, model);
            long started = System.currentTimeMillis();
            try {
                client.completeStructured("你是连接测试助手，只返回JSON。",
                        "请返回JSON对象 {\"ok\":true}", 256);
                long latency = System.currentTimeMillis() - started;
                return new AiTestResult(code, model, AiTestResult.SUCCESS, "连接与模型调用成功", latency);
            } catch (AiProviderException e) {
                long latency = System.currentTimeMillis() - started;
                return new AiTestResult(code, model, mapStatus(e.getCode()), e.getMessage(), latency);
            }
        } catch (Exception e) {
            log.warn("AI provider test failed: code={}, reason={}", code, e.getMessage());
            return new AiTestResult(code, model, AiTestResult.PROVIDER_ERROR, "Provider 异常：" + e.getMessage(), 0);
        }
    }

    private String mapStatus(String errorCode) {
        if ("AI_INVALID_KEY".equals(errorCode)) return AiTestResult.INVALID_KEY;
        if ("AI_MODEL_NOT_FOUND".equals(errorCode)) return AiTestResult.MODEL_NOT_FOUND;
        if ("AI_TIMEOUT".equals(errorCode)) return AiTestResult.TIMEOUT;
        if ("AI_RATE_LIMITED".equals(errorCode)) return AiTestResult.RATE_LIMITED;
        if ("AI_QUOTA_EXCEEDED".equals(errorCode)) return AiTestResult.QUOTA_EXCEEDED;
        if ("AI_NETWORK_ERROR".equals(errorCode)) return AiTestResult.NETWORK_ERROR;
        if ("AI_NOT_CONFIGURED".equals(errorCode)) return AiTestResult.NOT_CONFIGURED;
        return AiTestResult.PROVIDER_ERROR;
    }

    private void persistTestResult(String code, AiTestResult result) {
        try {
            configMapper.updateTestResult(code, result.getStatus(), result.getMessage(),
                    (int) result.getLatencyMs(), result.getModel());
        } catch (Exception e) {
            log.warn("保存测试结果失败: code={}, reason={}", code, e.getMessage());
        }
    }

    private AiProviderView view(String code, AiProviderConfig config) {
        AiProviderView view = new AiProviderView();
        view.setCode(code);
        view.setName(AiProviderDefaults.displayName(code));
        view.setDefaultBaseUrl(AiProviderDefaults.defaultBaseUrl(code));
        view.setDefaultModels(AiProviderDefaults.defaultModels(code));
        if (config == null) {
            view.setEnabled(false); view.setConfigured(false); view.setDefaultProvider(false);
            view.setModel(AiProviderDefaults.defaultModel(code)); view.setEffectiveModel(AiProviderDefaults.defaultModel(code));
            return view;
        }
        view.setEnabled(config.isEnabled());
        view.setConfigured(config.hasKey());
        view.setDefaultProvider(config.isDefaultProvider());
        view.setModel(config.getModel());
        view.setCustomModel(config.getCustomModel());
        view.setEffectiveModel(config.effectiveModel());
        view.setBaseUrl(config.getBaseUrl());
        view.setMaskedKey(config.getMaskedKey());
        view.setLastTestStatus(config.getLastTestStatus());
        view.setLastTestStatusText(config.getLastTestStatus() == null ? "未测试" : AiTestResult.statusText(config.getLastTestStatus()));
        view.setLastTestMessage(config.getLastTestMessage());
        view.setLastTestTime(config.getLastTestTime());
        view.setLastTestLatencyMs(config.getLastTestLatencyMs());
        view.setLastTestModel(config.getLastTestModel());
        return view;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
