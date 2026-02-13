package com.changye.web.service;

import com.changye.web.dto.request.PaperCreateRequest;
import com.changye.web.dto.response.PaperResponse;
import com.changye.web.model.Paper;
import com.changye.web.model.enums.ReadingStatus;
import com.changye.web.repository.NoteRepository;
import com.changye.web.repository.PaperRepository;
import com.changye.web.repository.TagRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperServiceTest {

    @Mock
    private PaperRepository paperRepository;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private TagRepository tagRepository;

    private PaperService paperService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        paperService = new PaperService(paperRepository, noteRepository, tagRepository, objectMapper);
        ReflectionTestUtils.setField(paperService, "uploadPath", tempDir.toString());
    }

    @Test
    void listPapersMapsResponse() throws Exception {
        Paper paper = Paper.builder()
                .id(1L)
                .title("Sample")
                .authors(objectMapper.writeValueAsString(List.of("Author A", "Author B")))
                .readingStatus(ReadingStatus.UNREAD)
                .starred(false)
                .build();
        paper.setCreatedAt(OffsetDateTime.now());
        paper.setUpdatedAt(OffsetDateTime.now());

        when(paperRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(paper)));
        when(noteRepository.countByPaperId(1L)).thenReturn(2L);

        Page<PaperResponse> result = paperService.listPapers("key", null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        PaperResponse response = result.getContent().get(0);
        assertThat(response.getAuthors()).containsExactly("Author A", "Author B");
        assertThat(response.getNoteCount()).isEqualTo(2);
        assertThat(response.getFilePath()).isEqualTo("/api/v1/papers/1/file");
    }

    @Test
    void createPaperStoresFile() throws Exception {
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> {
            Paper saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(1L);
            }
            return saved;
        });
        when(tagRepository.findAllById(anyList())).thenReturn(List.of());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "paper.pdf",
                "application/pdf",
                "dummy".getBytes()
        );

        PaperCreateRequest request = new PaperCreateRequest();
        request.setTitle("Title");

        PaperResponse response = paperService.createPaper(file, request);

        Path stored = tempDir.resolve("1_paper.pdf");
        assertThat(Files.exists(stored)).isTrue();
        assertThat(response.getFileName()).isEqualTo("paper.pdf");
        assertThat(response.getFileSize()).isEqualTo(file.getSize());
    }
}
