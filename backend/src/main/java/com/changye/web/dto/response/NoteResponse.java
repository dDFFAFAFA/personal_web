package com.changye.web.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class NoteResponse {

    private Long id;
    private Long paperId;
    private String title;
    private String content;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
