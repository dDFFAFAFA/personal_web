package com.changye.web.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperRepoLinksExtractResponse {
    private Long paperId;
    private Integer candidateCount;
    private List<PaperRepoLinkResponse> candidates;
}
