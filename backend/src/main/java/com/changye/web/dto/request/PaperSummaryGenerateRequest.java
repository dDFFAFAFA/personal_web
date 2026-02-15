package com.changye.web.dto.request;

import com.changye.web.model.enums.AiProvider;
import lombok.Data;

@Data
public class PaperSummaryGenerateRequest {
    private AiProvider provider;
    private Boolean forceRegenerate;
}
