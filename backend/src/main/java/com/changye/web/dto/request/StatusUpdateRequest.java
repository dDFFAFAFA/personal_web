package com.changye.web.dto.request;

import com.changye.web.model.enums.ReadingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotNull(message = "阅读状态不能为空")
    private ReadingStatus readingStatus;
}
