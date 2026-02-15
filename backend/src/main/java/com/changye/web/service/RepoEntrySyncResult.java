package com.changye.web.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepoEntrySyncResult {
    private Long entryId;
    private String repoUrl;
    private String localPath;
    private String action;
    private String message;
}
