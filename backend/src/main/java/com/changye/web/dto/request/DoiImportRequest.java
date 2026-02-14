package com.changye.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DoiImportRequest {

    @NotBlank(message = "DOI 不能为空")
    private String doi;
}
