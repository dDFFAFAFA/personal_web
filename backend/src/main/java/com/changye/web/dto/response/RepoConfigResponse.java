package com.changye.web.dto.response;

import com.changye.web.model.enums.RepoProvider;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepoConfigResponse {
    private RepoProvider provider;
    private String repoUrl;
    private String branch;
    private String targetDir;
    private Boolean autoCommitReadme;
    private String appBaseUrl;
    private String sshKeyPath;
    private boolean configured;
}
