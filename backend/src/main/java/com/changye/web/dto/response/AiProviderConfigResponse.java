package com.changye.web.dto.response;

import com.changye.web.model.enums.AiProvider;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiProviderConfigResponse {
    private AiProvider provider;
    private Boolean enabled;
    private String baseUrl;
    private String model;
    private String apiKeyMasked;
    private OffsetDateTime updatedAt;
}
