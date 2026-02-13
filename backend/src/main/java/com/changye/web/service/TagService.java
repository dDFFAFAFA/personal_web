package com.changye.web.service;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.TagCreateRequest;
import com.changye.web.dto.response.TagResponse;
import com.changye.web.model.Tag;
import com.changye.web.repository.PaperRepository;
import com.changye.web.repository.TagRepository;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final PaperRepository paperRepository;

    public TagService(TagRepository tagRepository, PaperRepository paperRepository) {
        this.tagRepository = tagRepository;
        this.paperRepository = paperRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listTags() {
        return tagRepository.findAll().stream()
                .map(tag -> TagResponse.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .color(tag.getColor())
                        .paperCount(paperRepository.countByTags_Id(tag.getId()))
                        .createdAt(tag.getCreatedAt())
                        .build())
                .toList();
    }

    public TagResponse createTag(TagCreateRequest request) {
        tagRepository.findByName(request.getName()).ifPresent(tag -> {
            throw new BusinessException(409, "标签名已存在");
        });

        Tag tag = Tag.builder()
                .name(request.getName())
                .color(StringUtils.hasText(request.getColor()) ? request.getColor() : "#409EFF")
                .build();
        Tag saved = tagRepository.save(tag);
        log.info("Created tag id={} name={}", saved.getId(), saved.getName());
        return TagResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .color(saved.getColor())
                .paperCount(0L)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public TagResponse updateTag(Long id, TagCreateRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "标签不存在"));

        tagRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException(409, "标签名已存在");
            }
        });

        tag.setName(request.getName());
        if (StringUtils.hasText(request.getColor())) {
            tag.setColor(request.getColor());
        }
        Tag saved = tagRepository.save(tag);
        log.info("Updated tag id={} name={}", saved.getId(), saved.getName());
        return TagResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .color(saved.getColor())
                .paperCount(paperRepository.countByTags_Id(saved.getId()))
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "标签不存在"));
        tagRepository.delete(tag);
        log.info("Deleted tag id={}", id);
    }
}
