package com.gkstudy.ai;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * AI 统一入口：业务模块（申论批改、错因分析、政治材料结构化、AI Coach）一律注入本服务，
 * 由 ProviderRegistry 决定当前生效 Provider。业务代码禁止判断具体厂商。
 */
@Primary
@Service
public class AiService implements AiProvider {
    private final ProviderRegistry registry;

    public AiService(ProviderRegistry registry) { this.registry = registry; }

    @Override
    public AiResponse completeStructured(String systemPrompt, String userPrompt, int maxTokens) {
        return registry.active().completeStructured(systemPrompt, userPrompt, maxTokens);
    }

    @Override
    public boolean healthCheck() { return registry.active().healthCheck(); }

    @Override public String providerCode() { return registry.active().providerCode(); }
    @Override public String currentModel() { return registry.active().currentModel(); }
}
