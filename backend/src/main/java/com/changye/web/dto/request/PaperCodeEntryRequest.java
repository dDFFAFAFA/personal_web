package com.changye.web.dto.request;

import com.changye.web.model.enums.RepoProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaperCodeEntryRequest {

    @NotNull(message = "论文ID不能为空")
    private Long paperId;

    @NotBlank(message = "代码仓库地址不能为空")
    @Size(max = 500)
    private String repoUrl;

    @Size(max = 100)
    private String branch;

    @NotNull(message = "代码仓库平台不能为空")
    private RepoProvider provider;

    @Size(max = 5000)
    private String description;
}
