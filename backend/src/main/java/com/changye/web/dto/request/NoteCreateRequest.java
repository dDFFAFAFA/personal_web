package com.changye.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoteCreateRequest {

    @NotBlank(message = "笔记标题不能为空")
    @Size(max = 200)
    private String title;

    private String content;
}
