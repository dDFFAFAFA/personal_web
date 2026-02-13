package com.changye.web.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoteUpdateRequest {

    @Size(max = 200)
    private String title;

    private String content;

    private Integer sortOrder;
}
