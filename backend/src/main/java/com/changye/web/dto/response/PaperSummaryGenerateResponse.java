package com.changye.web.dto.response;

import com.changye.web.model.enums.PaperSummaryStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperSummaryGenerateResponse {
    private Long paperId;
    private Long summaryId;
    private PaperSummaryStatus status;
    private String message;
}
