package com.changye.web.controller;

import java.util.List;
import lombok.Data;

@Data
public class RepoEntriesSyncRequest {
    private Long paperId;
    private List<Long> entryIds;
}
