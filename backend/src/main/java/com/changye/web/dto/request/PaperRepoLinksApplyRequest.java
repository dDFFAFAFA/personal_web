package com.changye.web.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class PaperRepoLinksApplyRequest {
    private List<Long> candidateIds;
    private Boolean autoRebuildReadme;
}
