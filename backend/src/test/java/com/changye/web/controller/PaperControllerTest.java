package com.changye.web.controller;

import com.changye.web.dto.request.PaperCreateRequest;
import com.changye.web.dto.response.PaperResponse;
import com.changye.web.model.enums.ReadingStatus;
import com.changye.web.service.PaperService;
import com.changye.web.service.StorageBackupService;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaperController.class)
class PaperControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaperService paperService;

    @MockBean
    private StorageBackupService storageBackupService;

    @Test
    void listPapersReturnsPage() throws Exception {
        PaperResponse paper = PaperResponse.builder()
                .id(1L)
                .title("Paper")
                .authors(List.of("Author"))
                .readingStatus(ReadingStatus.UNREAD)
                .starred(false)
                .noteCount(0)
                .build();

        Page<PaperResponse> page = new PageImpl<>(List.of(paper), PageRequest.of(0, 20), 1);
        when(paperService.listPapers(any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/papers")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Paper"));
    }

    @Test
    void createPaperReturnsResponse() throws Exception {
        PaperResponse response = PaperResponse.builder()
                .id(2L)
                .title("New Paper")
                .fileName("paper.pdf")
                .build();
        when(paperService.createPaper(any(), any(PaperCreateRequest.class)))
                .thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "paper.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "data".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/papers")
                        .file(file)
                        .param("title", "New Paper")
                        .param("authors", "[\"Author A\"]")
                        .param("year", "2024")
                        .param("tagIds", "[1,2]")
                        .param("venue", "NeurIPS")
                        .param("doi", "10.1234")
                        .param("abstractText", "Abstract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.title").value("New Paper"));
    }

    @Test
    void getPaperReturnsDetail() throws Exception {
        PaperResponse response = PaperResponse.builder()
                .id(3L)
                .title("Detail")
                .build();
        when(paperService.getPaper(3L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/papers/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.title").value("Detail"));
    }
}
