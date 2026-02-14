package com.changye.web.dto.response;

import com.changye.web.model.enums.AiProvider;
import com.changye.web.model.enums.PaperSummaryStatus;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperSummaryResponse {
    private Long summaryId;
    private Long paperId;
    private PaperSummaryStatus status;
    private AiProvider provider;
    private String model;
    private String markdown;
    private OffsetDateTime generatedAt;
    private String error;
}
