package com.changye.web.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperRepoLinksApplyResponse {
    private Long paperId;
    private Integer appliedCount;
    private List<Long> appliedCandidateIds;
}
