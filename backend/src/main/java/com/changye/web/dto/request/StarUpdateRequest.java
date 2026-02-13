package com.changye.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StarUpdateRequest {

    @NotNull(message = "starred 不能为空")
    private Boolean starred;
}
