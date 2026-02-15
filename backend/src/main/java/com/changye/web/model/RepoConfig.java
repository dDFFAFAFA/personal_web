package com.changye.web.model;

import com.changye.web.model.enums.RepoProvider;
import com.changye.web.model.enums.RepoSyncMode;
import com.changye.web.model.enums.RepoSyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "repo_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepoConfig {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20)
    private RepoProvider provider;

    @Column(name = "repo_url", length = 500)
    private String repoUrl;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "target_dir", length = 500)
    private String targetDir;

    @Column(name = "auto_commit_readme", nullable = false)
    @Builder.Default
    private Boolean autoCommitReadme = true;

    @Column(name = "app_base_url", length = 500)
    private String appBaseUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sync_status", length = 20)
    @Builder.Default
    private RepoSyncStatus lastSyncStatus = RepoSyncStatus.IDLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sync_mode", length = 20)
    private RepoSyncMode lastSyncMode;

    @Column(name = "last_sync_message", columnDefinition = "TEXT")
    private String lastSyncMessage;

    @Column(name = "last_sync_at")
    private OffsetDateTime lastSyncAt;

    @Column(name = "last_sync_error_code")
    private Integer lastSyncErrorCode;

    @Column(name = "last_sync_duration_ms")
    private Long lastSyncDurationMs;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
