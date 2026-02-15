package com.changye.web.dto.request;

import com.changye.web.model.enums.RepoProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RepoConfigUpdateRequest {

    @NotNull(message = "仓库平台不能为空")
    private RepoProvider provider;

    @NotBlank(message = "仓库地址不能为空")
    @Size(max = 500)
    private String repoUrl;

    @NotBlank(message = "分支不能为空")
    @Size(max = 100)
    private String branch;

    @NotBlank(message = "目标目录不能为空")
    @Size(max = 500)
    private String targetDir;

    private Boolean autoCommitReadme;

    @Size(max = 500)
    private String appBaseUrl;
}
