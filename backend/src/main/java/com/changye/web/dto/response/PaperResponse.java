package com.changye.web.dto.response;

import com.changye.web.model.enums.ReadingStatus;
import com.changye.web.model.enums.BackupStatus;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class PaperResponse {

    private Long id;
    private String title;
    private List<String> authors;
    private Integer year;
    private String venue;
    private String doi;
    private String fileName;
    private Long fileSize;
    private String filePath; // API path: /api/v1/papers/{id}/file
    private ReadingStatus readingStatus;
    private Boolean starred;
    private String abstractText;

    // Phase 2A: Metadata Enrichment
    private String ccfRank;
    private String jcrQuartile;
    private BigDecimal impactFactor;
    private Integer citationCount;
    private String paperUrl;
    private BackupStatus backupStatus;
    private OffsetDateTime backupAt;
    private String backupError;

    private List<TagResponse> tags;
    private List<NoteSummaryResponse> notes; // Only in detail view
    private Integer noteCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * Lightweight note summary for paper detail view (without full content)
     */
    @Data
    @Builder
    public static class NoteSummaryResponse {
        private Long id;
        private String title;
        private Integer sortOrder;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }
}
