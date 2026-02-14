package com.changye.web.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiProviderConfigUpdateRequest {

    private Boolean enabled;

    @Size(max = 500)
    private String baseUrl;

    @Size(max = 120)
    private String model;

    @Size(max = 500)
    private String apiKey;
}
