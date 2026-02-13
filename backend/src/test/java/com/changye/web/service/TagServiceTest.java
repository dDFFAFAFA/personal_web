package com.changye.web.service;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.TagCreateRequest;
import com.changye.web.model.Tag;
import com.changye.web.repository.PaperRepository;
import com.changye.web.repository.TagRepository;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private PaperRepository paperRepository;

    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagService(tagRepository, paperRepository);
    }

    @Test
    void createTagThrowsWhenDuplicateName() {
        Tag existing = Tag.builder().id(1L).name("NLP").build();
        when(tagRepository.findByName("NLP")).thenReturn(Optional.of(existing));

        TagCreateRequest request = new TagCreateRequest();
        request.setName("NLP");
        request.setColor("#FFFFFF");

        BusinessException ex = assertThrows(BusinessException.class, () -> tagService.createTag(request));
        assertThat(ex.getCode()).isEqualTo(409);
    }
}
