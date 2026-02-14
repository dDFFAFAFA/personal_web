package com.changye.web.dto.response;

import com.changye.web.model.enums.PaperRepoLinkProvider;
import com.changye.web.model.enums.PaperRepoLinkStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperRepoLinkResponse {
    private Long id;
    private Long paperId;
    private String url;
    private PaperRepoLinkProvider provider;
    private PaperRepoLinkStatus status;
    private String sourceText;
    private Integer pageNo;
    private BigDecimal confidence;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
