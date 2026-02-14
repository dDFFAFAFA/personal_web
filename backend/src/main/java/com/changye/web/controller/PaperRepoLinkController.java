package com.changye.web.controller;

import com.changye.web.common.ApiResponse;
import com.changye.web.dto.response.PaperRepoLinkResponse;
import com.changye.web.service.PaperRepoLinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/papers/{paperId}/repo-links")
public class PaperRepoLinkController {

    private final PaperRepoLinkService paperRepoLinkService;

    public PaperRepoLinkController(PaperRepoLinkService paperRepoLinkService) {
        this.paperRepoLinkService = paperRepoLinkService;
    }

    @DeleteMapping("/{candidateId}")
    public ResponseEntity<ApiResponse<PaperRepoLinkResponse>> rejectCandidate(
            @PathVariable Long paperId,
            @PathVariable Long candidateId) {
        return ResponseEntity.ok(ApiResponse.success("已拒绝", paperRepoLinkService.rejectLink(paperId, candidateId)));
    }
}
