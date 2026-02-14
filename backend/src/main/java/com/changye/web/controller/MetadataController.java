package com.changye.web.controller;

import com.changye.web.common.ApiResponse;
import com.changye.web.dto.request.DoiImportRequest;
import com.changye.web.dto.response.MetadataEnrichResponse;
import com.changye.web.dto.response.VenueRankingResponse;
import com.changye.web.service.MetadataService;
import com.changye.web.service.VenueRankingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Validated
public class MetadataController {

    private final MetadataService metadataService;
    private final VenueRankingService venueRankingService;

    public MetadataController(MetadataService metadataService, VenueRankingService venueRankingService) {
        this.metadataService = metadataService;
        this.venueRankingService = venueRankingService;
    }

    @PostMapping("/papers/enrich/doi")
    public ResponseEntity<ApiResponse<MetadataEnrichResponse>> enrichByDoi(
            @Valid @RequestBody DoiImportRequest request) {
        MetadataEnrichResponse response = metadataService.enrichByDoi(request.getDoi());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/papers/enrich/title")
    public ResponseEntity<ApiResponse<MetadataEnrichResponse>> enrichByTitle(
            @Valid @RequestBody TitleRequest request) {
        MetadataEnrichResponse response = metadataService.enrichByTitle(request.getTitle());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/papers/{id}/enrich")
    public ResponseEntity<ApiResponse<MetadataEnrichResponse>> enrichExisting(@PathVariable Long id) {
        MetadataEnrichResponse response = metadataService.enrichAndApply(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/venues/lookup")
    public ResponseEntity<ApiResponse<VenueRankingResponse>> lookupVenue(
            @RequestParam("name") @NotBlank String name) {
        VenueRankingResponse response = venueRankingService.lookup(name);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Data
    public static class TitleRequest {
        @NotBlank(message = "标题不能为空")
        private String title;
    }
}
