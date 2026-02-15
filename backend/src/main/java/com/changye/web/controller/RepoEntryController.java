package com.changye.web.controller;

import com.changye.web.common.ApiResponse;
import com.changye.web.dto.request.PaperCodeEntryRequest;
import com.changye.web.dto.response.PaperCodeEntryResponse;
import com.changye.web.service.PaperCodeEntryService;
import com.changye.web.service.RepoEntrySyncResult;
import com.changye.web.service.RepoEntrySyncService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/repo")
public class RepoEntryController {

    private final PaperCodeEntryService paperCodeEntryService;
    private final RepoEntrySyncService repoEntrySyncService;

    public RepoEntryController(PaperCodeEntryService paperCodeEntryService,
                               RepoEntrySyncService repoEntrySyncService) {
        this.paperCodeEntryService = paperCodeEntryService;
        this.repoEntrySyncService = repoEntrySyncService;
    }

    @GetMapping("/code-entries")
    public ResponseEntity<ApiResponse<List<PaperCodeEntryResponse>>> listEntries(
            @RequestParam(value = "paperId", required = false) Long paperId) {
        List<PaperCodeEntryResponse> data = paperId == null
                ? paperCodeEntryService.listEntries()
                : paperCodeEntryService.listEntriesByPaper(paperId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/code-entries")
    public ResponseEntity<ApiResponse<PaperCodeEntryResponse>> createEntry(@Valid @RequestBody PaperCodeEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("创建成功", paperCodeEntryService.createEntry(request)));
    }

    @PutMapping("/code-entries/{id}")
    public ResponseEntity<ApiResponse<PaperCodeEntryResponse>> updateEntry(@PathVariable Long id,
                                                                            @Valid @RequestBody PaperCodeEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("更新成功", paperCodeEntryService.updateEntry(id, request)));
    }

    @DeleteMapping("/code-entries/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEntry(@PathVariable Long id) {
        paperCodeEntryService.deleteEntry(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PostMapping("/entries/sync")
    public ResponseEntity<ApiResponse<List<RepoEntrySyncResult>>> syncEntries(
            @RequestBody(required = false) RepoEntriesSyncRequest request) {
        Long paperId = request == null ? null : request.getPaperId();
        List<Long> entryIds = request == null ? null : request.getEntryIds();
        return ResponseEntity.ok(ApiResponse.success(repoEntrySyncService.syncEntries(paperId, entryIds)));
    }

    @PostMapping("/readme/rebuild")
    public ResponseEntity<ApiResponse<Void>> rebuildReadme() {
        paperCodeEntryService.rebuildReadme();
        return ResponseEntity.ok(ApiResponse.success("README 已重建并尝试同步", null));
    }
}
