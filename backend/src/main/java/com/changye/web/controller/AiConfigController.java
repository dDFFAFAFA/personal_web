package com.changye.web.controller;

import com.changye.web.common.ApiResponse;
import com.changye.web.dto.request.AiProviderConfigUpdateRequest;
import com.changye.web.dto.response.AiProviderConfigResponse;
import com.changye.web.model.enums.AiProvider;
import com.changye.web.service.AiProviderConfigService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiConfigController {

    private final AiProviderConfigService aiProviderConfigService;

    public AiConfigController(AiProviderConfigService aiProviderConfigService) {
        this.aiProviderConfigService = aiProviderConfigService;
    }

    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<AiProviderConfigResponse>>> listConfigs() {
        return ResponseEntity.ok(ApiResponse.success(aiProviderConfigService.listConfigs()));
    }

    @PutMapping("/providers/{provider}")
    public ResponseEntity<ApiResponse<AiProviderConfigResponse>> updateConfig(
            @PathVariable AiProvider provider,
            @Valid @RequestBody AiProviderConfigUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("保存成功", aiProviderConfigService.updateConfig(provider, request)));
    }
}
