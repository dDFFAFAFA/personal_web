package com.changye.web.controller;

import com.changye.web.common.ApiResponse;
import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.PaperCreateRequest;
import com.changye.web.dto.response.PaperResponse;
import com.changye.web.service.BibTexService;
import com.changye.web.service.PaperService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Validated
public class ImportExportController {

    private final BibTexService bibTexService;
    private final PaperService paperService;

    public ImportExportController(BibTexService bibTexService, PaperService paperService) {
        this.bibTexService = bibTexService;
        this.paperService = paperService;
    }

    @GetMapping("/papers/export")
    public ResponseEntity<Resource> exportPapers(
            @RequestParam("format") String format,
            @RequestParam("ids") String ids) {
        List<Long> paperIds = parseIds(ids);
        String normalized = format == null ? "" : format.trim().toLowerCase(Locale.ROOT);
        String content;
        String extension;
        MediaType mediaType;

        if ("bibtex".equals(normalized)) {
            content = bibTexService.exportBibTeX(paperIds);
            extension = "bib";
            mediaType = MediaType.valueOf("application/x-bibtex");
        } else if ("ris".equals(normalized)) {
            content = bibTexService.exportRis(paperIds);
            extension = "ris";
            mediaType = MediaType.valueOf("application/x-research-info-systems");
        } else {
            throw new BusinessException(400, "不支持的导出格式");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = "papers_" + timestamp + "." + extension;
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Resource resource = new ByteArrayResource(bytes);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(bytes.length)
                .body(resource);
    }

    @PostMapping(value = "/papers/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<PaperResponse>>> importPapers(
            @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        String filename = file.getOriginalFilename();
        String lowerName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        List<PaperCreateRequest> requests;
        try {
            if (lowerName.endsWith(".ris")) {
                requests = bibTexService.parseRis(file.getInputStream());
            } else if (lowerName.endsWith(".bib") || lowerName.endsWith(".bibtex")) {
                requests = bibTexService.parseBibTeX(file.getInputStream());
            } else {
                throw new BusinessException(400, "不支持的文件格式");
            }
        } catch (Exception ex) {
            if (ex instanceof BusinessException) {
                throw (BusinessException) ex;
            }
            log.warn("Failed to import bibliography file", ex);
            throw new BusinessException(400, "导入失败");
        }

        List<PaperResponse> responses = paperService.createPapersFromImport(requests);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    private List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            throw new BusinessException(400, "ids 不能为空");
        }
        try {
            return Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, "ids 格式错误");
        }
    }
}
