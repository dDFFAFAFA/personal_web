package com.changye.web.dto.response;

import com.changye.web.model.enums.RepoProvider;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperCodeEntryResponse {
    private Long id;
    private Long paperId;
    private String paperTitle;
    private String repoUrl;
    private String branch;
    private RepoProvider provider;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
