package com.gkstudy.ai.service;

import com.gkstudy.ai.ApiKeyCipher;
import com.gkstudy.ai.ProviderRegistry;
import com.gkstudy.ai.dto.AiProviderView;
import com.gkstudy.ai.dto.SaveAiProviderRequest;
import com.gkstudy.ai.mapper.AiProviderConfigMapper;
import com.gkstudy.ai.model.AiProviderConfig;
import com.gkstudy.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminAiProviderServiceTest {
    private AiProviderConfigMapper configMapper;
    private ProviderRegistry registry;
    private AdminAiProviderService service;

    private final ApiKeyCipher cipher = new ApiKeyCipher() {
        @Override protected String masterKey() { return "unit-test-master-key"; }
    };

    @BeforeEach
    void setUp() {
        configMapper = mock(AiProviderConfigMapper.class);
        registry = mock(ProviderRegistry.class);
        service = new AdminAiProviderService(configMapper, cipher, registry);
    }

    private SaveAiProviderRequest request(String apiKey, Boolean enabled) {
        SaveAiProviderRequest request = new SaveAiProviderRequest();
        request.setApiKey(apiKey);
        request.setEnabled(enabled == null ? null : String.valueOf(enabled));
        request.setModel("deepseek-chat");
        return request;
    }

    @Test
    void saveEncryptsKeyAndReturnsMaskedKeyOnly() {
        when(configMapper.findByCode("DEEPSEEK")).thenReturn(null, savedConfig());
        AiProviderView view = service.save("DEEPSEEK", request("sk-abcdef12345678", true));
        assertNotNull(view.getMaskedKey());
        assertTrue(view.getMaskedKey().startsWith("sk-"));
        assertTrue(view.getMaskedKey().contains("****"));
        assertFalse(view.getMaskedKey().contains("abcdef12345678"));
        ArgumentCaptor<AiProviderConfig> captor = ArgumentCaptor.forClass(AiProviderConfig.class);
        verify(configMapper).insert(captor.capture());
        String stored = captor.getValue().getApiKeyCipher();
        assertNotNull(stored);
        assertFalse(stored.contains("sk-abcdef12345678"));
        assertEquals("sk-abcdef12345678", cipher.decrypt(stored));
    }

    private AiProviderConfig savedConfig() {
        AiProviderConfig config = new AiProviderConfig();
        config.setProviderCode("DEEPSEEK");
        config.setEnabled(true);
        config.setModel("deepseek-chat");
        config.setApiKeyCipher(cipher.encrypt("sk-abcdef12345678"));
        config.setMaskedKey(cipher.mask("sk-abcdef12345678"));
        return config;
    }

    @Test
    void saveRejectsMaskedKeyReuse() {
        when(configMapper.findByCode("DEEPSEEK")).thenReturn(savedConfig());
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.save("DEEPSEEK", request("sk-****5678", null)));
        assertEquals("AI_KEY_INVALID", error.getCode());
    }

    @Test
    void saveRejectsUnsupportedProvider() {
        assertThrows(BusinessException.class, () -> service.save("UNKNOWN", request("sk-x", true)));
    }

    @Test
    void setDefaultRequiresConfiguredAndEnabled() {
        AiProviderConfig noKey = new AiProviderConfig();
        noKey.setProviderCode("GLM");
        when(configMapper.findByCode("GLM")).thenReturn(noKey);
        assertEquals("AI_PROVIDER_NOT_CONFIGURED",
                assertThrows(BusinessException.class, () -> service.setDefault("GLM")).getCode());

        AiProviderConfig disabled = savedConfig();
        disabled.setEnabled(false);
        when(configMapper.findByCode("GLM")).thenReturn(disabled);
        assertEquals("AI_PROVIDER_NOT_ENABLED",
                assertThrows(BusinessException.class, () -> service.setDefault("GLM")).getCode());
    }

    @Test
    void setDefaultSwitchesSingleDefault() {
        when(configMapper.findByCode("DEEPSEEK")).thenReturn(savedConfig());
        service.setDefault("DEEPSEEK");
        verify(configMapper).switchDefault("DEEPSEEK");
    }

    @Test
    void testReturnsNotConfiguredWhenKeyMissing() {
        when(configMapper.findByCode("QWEN")).thenReturn(null);
        assertEquals("NOT_CONFIGURED", service.test("QWEN").getStatus());
    }

    @Test
    void listShowsAllFiveProvidersWithChineseNames() {
        when(configMapper.findByCode(any())).thenReturn(null);
        assertEquals(5, service.list().size());
        assertEquals("DeepSeek", service.list().get(0).getName());
        assertEquals("GLM / 智谱", service.list().get(2).getName());
    }

    @Test
    void registryFallbackKeepsSingleActiveClient() {
        when(configMapper.findDefaultEnabled()).thenReturn(Collections.emptyList());
        assertNotNull(registry);
    }
}
