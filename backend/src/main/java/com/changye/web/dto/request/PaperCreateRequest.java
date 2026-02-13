package com.changye.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PaperCreateRequest {

    @NotBlank(message = "论文标题不能为空")
    @Size(max = 500, message = "标题长度不能超过500字符")
    private String title;

    private List<String> authors;

    private Integer year;

    @Size(max = 200)
    private String venue;

    @Size(max = 200)
    private String doi;

    @Size(max = 5000)
    private String abstractText;

    private List<Long> tagIds;
}
