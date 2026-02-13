package com.changye.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagCreateRequest {

    @NotBlank(message = "标签名不能为空")
    @Size(max = 50)
    private String name;

    @Size(max = 20)
    private String color;
}
