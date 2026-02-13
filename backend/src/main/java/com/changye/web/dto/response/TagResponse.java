package com.changye.web.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class TagResponse {

    private Long id;
    private String name;
    private String color;
    private Long paperCount;
    private OffsetDateTime createdAt;
}
