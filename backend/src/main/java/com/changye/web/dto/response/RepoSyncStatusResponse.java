package com.changye.web.dto.response;

import com.changye.web.model.enums.RepoSyncMode;
import com.changye.web.model.enums.RepoSyncStatus;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepoSyncStatusResponse {
    private RepoSyncStatus status;
    private RepoSyncMode mode;
    private String message;
    private OffsetDateTime syncedAt;
    private Integer errorCode;
    private Long durationMs;
}
