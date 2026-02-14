package com.changye.web.service;

import com.changye.web.dto.request.AiProviderConfigUpdateRequest;
import com.changye.web.dto.response.AiProviderConfigResponse;
import com.changye.web.model.AiProviderConfig;
import com.changye.web.model.enums.AiProvider;
import com.changye.web.repository.AiProviderConfigRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@Transactional
public class AiProviderConfigService {

    private final AiProviderConfigRepository aiProviderConfigRepository;

    public AiProviderConfigService(AiProviderConfigRepository aiProviderConfigRepository) {
        this.aiProviderConfigRepository = aiProviderConfigRepository;
    }

    @Transactional(readOnly = true)
    public List<AiProviderConfigResponse> listConfigs() {
        List<AiProviderConfigResponse> responses = new ArrayList<>();
        responses.add(toResponse(getOrDefault(AiProvider.DEEPSEEK)));
        responses.add(toResponse(getOrDefault(AiProvider.QWEN)));
        return responses;
    }

    public AiProviderConfigResponse updateConfig(AiProvider provider, AiProviderConfigUpdateRequest request) {
        AiProviderConfig config = aiProviderConfigRepository.findById(provider)
                .orElseGet(() -> defaultConfig(provider));

        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }
        if (StringUtils.hasText(request.getBaseUrl())) {
            config.setBaseUrl(request.getBaseUrl().trim());
        }
        if (StringUtils.hasText(request.getModel())) {
            config.setModelName(request.getModel().trim());
        }
        if (StringUtils.hasText(request.getApiKey())) {
            config.setApiKey(request.getApiKey().trim());
        }

        AiProviderConfig saved = aiProviderConfigRepository.save(config);
        log.info("AI provider config updated provider={} enabled={}", provider, saved.getEnabled());
        return toResponse(saved);
    }

    private AiProviderConfig getOrDefault(AiProvider provider) {
        return aiProviderConfigRepository.findById(provider)
                .orElseGet(() -> defaultConfig(provider));
    }

    private AiProviderConfig defaultConfig(AiProvider provider) {
        if (provider == AiProvider.DEEPSEEK) {
            return AiProviderConfig.builder()
                    .provider(provider)
                    .enabled(false)
                    .baseUrl("https://api.deepseek.com/v1")
                    .modelName("deepseek-chat")
                    .build();
        }
        return AiProviderConfig.builder()
                .provider(provider)
                .enabled(false)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .modelName("qwen-plus")
                .build();
    }

    private AiProviderConfigResponse toResponse(AiProviderConfig config) {
        return AiProviderConfigResponse.builder()
                .provider(config.getProvider())
                .enabled(config.getEnabled())
                .baseUrl(config.getBaseUrl())
                .model(config.getModelName())
                .apiKeyMasked(maskApiKey(config.getApiKey()))
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return "";
        }
        int len = apiKey.length();
        if (len <= 6) {
            return "***";
        }
        return apiKey.substring(0, 3) + "***" + apiKey.substring(len - 3);
    }
}
