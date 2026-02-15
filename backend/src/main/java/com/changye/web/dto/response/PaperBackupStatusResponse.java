package com.changye.web.dto.response;

import com.changye.web.model.enums.BackupStatus;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperBackupStatusResponse {
    private Long paperId;
    private BackupStatus backupStatus;
    private OffsetDateTime backupAt;
    private String backupError;
    private String ossObjectKey;
}
