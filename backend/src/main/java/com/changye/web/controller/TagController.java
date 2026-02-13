package com.changye.web.controller;

import com.changye.web.common.ApiResponse;
import com.changye.web.dto.request.TagCreateRequest;
import com.changye.web.dto.response.TagResponse;
import com.changye.web.service.TagService;

import jakarta.validation.Valid;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Validated
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> listTags() {
        List<TagResponse> response = tagService.listTags();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/tags")
    public ResponseEntity<ApiResponse<TagResponse>> createTag(
            @Valid @RequestBody TagCreateRequest request) {
        TagResponse response = tagService.createTag(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/tags/{id}")
    public ResponseEntity<ApiResponse<TagResponse>> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagCreateRequest request) {
        TagResponse response = tagService.updateTag(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/tags/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
