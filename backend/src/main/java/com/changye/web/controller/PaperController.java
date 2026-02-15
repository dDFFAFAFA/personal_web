package com.changye.web.controller;

import com.changye.web.common.ApiResponse;
import com.changye.web.common.PageResponse;
import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.PaperRepoLinksApplyRequest;
import com.changye.web.dto.request.PaperCreateRequest;
import com.changye.web.dto.request.PaperSummaryGenerateRequest;
import com.changye.web.dto.request.PaperUpdateRequest;
import com.changye.web.dto.request.StarUpdateRequest;
import com.changye.web.dto.request.StatusUpdateRequest;
import com.changye.web.dto.response.PaperBackupStatusResponse;
import com.changye.web.dto.response.PaperRepoLinkResponse;
import com.changye.web.dto.response.PaperRepoLinksApplyResponse;
import com.changye.web.dto.response.PaperRepoLinksExtractResponse;
import com.changye.web.dto.response.PaperResponse;
import com.changye.web.dto.response.PaperSummaryGenerateResponse;
import com.changye.web.dto.response.PaperSummaryResponse;
import com.changye.web.model.enums.ReadingStatus;
import com.changye.web.service.PaperService;
import com.changye.web.service.PaperRepoLinkService;
import com.changye.web.service.PaperSummaryService;
import com.changye.web.service.StorageBackupService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Validated
public class PaperController {

    private final PaperService paperService;
    private final StorageBackupService storageBackupService;
    private final PaperSummaryService paperSummaryService;
    private final PaperRepoLinkService paperRepoLinkService;
    private final ObjectMapper objectMapper;

    public PaperController(PaperService paperService,
                           StorageBackupService storageBackupService,
                           PaperSummaryService paperSummaryService,
                           PaperRepoLinkService paperRepoLinkService,
                           ObjectMapper objectMapper) {
        this.paperService = paperService;
        this.storageBackupService = storageBackupService;
        this.paperSummaryService = paperSummaryService;
        this.paperRepoLinkService = paperRepoLinkService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/papers")
    public ResponseEntity<ApiResponse<PageResponse<PaperResponse>>> listPapers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ReadingStatus status,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Boolean starred,
            Pageable pageable) {
        Page<PaperResponse> page = paperService.listPapers(keyword, status, tagId, starred, pageable);
        PageResponse<PaperResponse> data = PageResponse.<PaperResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/home/summaries")
    public ResponseEntity<ApiResponse<List<PaperSummaryResponse>>> listHomeSummaries(
            @RequestParam(defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(ApiResponse.success(paperSummaryService.listHomeSummaries(limit)));
    }

    @PostMapping(value = "/papers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PaperResponse>> createPaper(
            @RequestPart("file") MultipartFile file,
            @RequestParam("title") @NotBlank @Size(max = 500) String title,
            @RequestParam(value = "authors", required = false) String authors,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "venue", required = false) @Size(max = 200) String venue,
            @RequestParam(value = "doi", required = false) @Size(max = 200) String doi,
            @RequestParam(value = "abstractText", required = false) @Size(max = 5000) String abstractText,
            @RequestParam(value = "tagIds", required = false) String tagIds) {
        PaperCreateRequest request = new PaperCreateRequest();
        request.setTitle(title);
        request.setAuthors(parseList(authors, new TypeReference<List<String>>() {}));
        request.setYear(year);
        request.setVenue(venue);
        request.setDoi(doi);
        request.setAbstractText(abstractText);
        request.setTagIds(parseList(tagIds, new TypeReference<List<Long>>() {}));

        PaperResponse response = paperService.createPaper(file, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/papers/{id}")
    public ResponseEntity<ApiResponse<PaperResponse>> getPaper(@PathVariable Long id) {
        PaperResponse response = paperService.getPaper(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/papers/{id}")
    public ResponseEntity<ApiResponse<PaperResponse>> updatePaper(
            @PathVariable Long id,
            @Validated @RequestBody PaperUpdateRequest request) {
        PaperResponse response = paperService.updatePaper(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/papers/{id}/status")
    public ResponseEntity<ApiResponse<PaperResponse>> updateStatus(
            @PathVariable Long id,
            @Validated @RequestBody StatusUpdateRequest request) {
        PaperResponse response = paperService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/papers/{id}/star")
    public ResponseEntity<ApiResponse<PaperResponse>> toggleStar(
            @PathVariable Long id,
            @Validated @RequestBody StarUpdateRequest request) {
        PaperResponse response = paperService.toggleStar(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/papers/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePaper(@PathVariable Long id) {
        paperService.deletePaper(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @GetMapping("/papers/{id}/file")
    public ResponseEntity<Resource> getPaperFile(@PathVariable Long id) {
        Resource resource = paperService.getPaperFile(id);
        String filename = resource.getFilename() == null ? "paper.pdf" : resource.getFilename();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    @PostMapping("/papers/{id}/backup")
    public ResponseEntity<ApiResponse<PaperBackupStatusResponse>> backupPaper(@PathVariable Long id) {
        PaperBackupStatusResponse response = storageBackupService.backupPaper(id);
        return ResponseEntity.ok(ApiResponse.success("备份成功", response));
    }

    @GetMapping("/papers/{id}/backup-status")
    public ResponseEntity<ApiResponse<PaperBackupStatusResponse>> getBackupStatus(@PathVariable Long id) {
        PaperBackupStatusResponse response = storageBackupService.getBackupStatus(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/papers/{id}/restore")
    public ResponseEntity<ApiResponse<PaperBackupStatusResponse>> restorePaper(@PathVariable Long id) {
        PaperBackupStatusResponse response = storageBackupService.restorePaper(id);
        return ResponseEntity.ok(ApiResponse.success("恢复成功", response));
    }

    @PostMapping("/papers/{id}/summary/generate")
    public ResponseEntity<ApiResponse<PaperSummaryGenerateResponse>> generateSummary(
            @PathVariable Long id,
            @RequestBody(required = false) PaperSummaryGenerateRequest request) {
        PaperSummaryGenerateResponse response = paperSummaryService.generateSummary(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/papers/{id}/summary")
    public ResponseEntity<ApiResponse<PaperSummaryResponse>> getSummary(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(paperSummaryService.getSummary(id)));
    }

    @GetMapping("/papers/{id}/summary/download")
    public ResponseEntity<ByteArrayResource> downloadSummary(@PathVariable Long id) {
        String markdown = paperSummaryService.getSummaryMarkdown(id);
        String filename = "paper_" + id + "_summary.md";
        ByteArrayResource resource = new ByteArrayResource(markdown.getBytes());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @PostMapping("/papers/{id}/repo-links/extract")
    public ResponseEntity<ApiResponse<PaperRepoLinksExtractResponse>> extractRepoLinks(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(paperRepoLinkService.extractLinks(id)));
    }

    @GetMapping("/papers/{id}/repo-links")
    public ResponseEntity<ApiResponse<List<PaperRepoLinkResponse>>> listRepoLinks(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(paperRepoLinkService.listLinks(id)));
    }

    @PostMapping("/papers/{id}/repo-links/apply")
    public ResponseEntity<ApiResponse<PaperRepoLinksApplyResponse>> applyRepoLinks(
            @PathVariable Long id,
            @RequestBody(required = false) PaperRepoLinksApplyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("应用成功", paperRepoLinkService.applyLinks(id, request)));
    }

    private <T> List<T> parseList(String raw, TypeReference<List<T>> type) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, type);
        } catch (Exception ex) {
            log.warn("Failed to parse list payload: {}", raw, ex);
            throw new BusinessException(400, "列表格式错误");
        }
    }
}
