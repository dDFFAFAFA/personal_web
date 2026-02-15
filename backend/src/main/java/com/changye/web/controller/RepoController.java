package com.changye.web.controller;

import com.changye.web.common.ApiResponse;
import com.changye.web.dto.request.RepoConfigUpdateRequest;
import com.changye.web.dto.response.RepoConfigResponse;
import com.changye.web.dto.response.RepoSyncStatusResponse;
import com.changye.web.service.RepoSyncService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/repo")
public class RepoController {

    private final RepoSyncService repoSyncService;

    public RepoController(RepoSyncService repoSyncService) {
        this.repoSyncService = repoSyncService;
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<RepoConfigResponse>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(repoSyncService.getConfig()));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<RepoConfigResponse>> updateConfig(@Valid @RequestBody RepoConfigUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("保存成功", repoSyncService.updateConfig(request)));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<RepoSyncStatusResponse>> sync() {
        return ResponseEntity.ok(ApiResponse.success(repoSyncService.sync()));
    }

    @GetMapping("/sync-status")
    public ResponseEntity<ApiResponse<RepoSyncStatusResponse>> getSyncStatus() {
        return ResponseEntity.ok(ApiResponse.success(repoSyncService.getSyncStatus()));
    }
}
