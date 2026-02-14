package com.changye.web.controller;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.PaperCreateRequest;
import com.changye.web.dto.response.PaperBackupStatusResponse;
import com.changye.web.dto.response.PaperResponse;
import com.changye.web.model.enums.BackupStatus;
import com.changye.web.model.enums.ReadingStatus;
import com.changye.web.service.PaperService;
import com.changye.web.service.StorageBackupService;

import java.time.OffsetDateTime;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void backupPaperReturnsSuccess() throws Exception {
        PaperBackupStatusResponse response = PaperBackupStatusResponse.builder()
                .paperId(5L)
                .backupStatus(BackupStatus.BACKED_UP)
                .backupAt(OffsetDateTime.now())
                .ossObjectKey("papers/5/paper.pdf")
                .build();
        when(storageBackupService.backupPaper(5L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/papers/5/backup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("备份成功"))
                .andExpect(jsonPath("$.data.paperId").value(5))
                .andExpect(jsonPath("$.data.backupStatus").value("BACKED_UP"));
    }

    @Test
    void backupPaperReturnsBusinessError() throws Exception {
        doThrow(new BusinessException(403, "OSS 访问被拒绝"))
                .when(storageBackupService).backupPaper(6L);

        mockMvc.perform(post("/api/v1/papers/6/backup"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("OSS 访问被拒绝"));
    }

    @Test
    void backupStatusReturnsSuccess() throws Exception {
        PaperBackupStatusResponse response = PaperBackupStatusResponse.builder()
                .paperId(7L)
                .backupStatus(BackupStatus.FAILED)
                .backupError("OSS 服务异常")
                .build();
        when(storageBackupService.getBackupStatus(7L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/papers/7/backup-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.paperId").value(7))
                .andExpect(jsonPath("$.data.backupStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.backupError").value("OSS 服务异常"));
    }

    @Test
    void backupStatusReturnsNotFound() throws Exception {
        doThrow(new BusinessException(404, "论文不存在"))
                .when(storageBackupService).getBackupStatus(8L);

        mockMvc.perform(get("/api/v1/papers/8/backup-status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("论文不存在"));
    }

    @Test
    void restorePaperReturnsSuccess() throws Exception {
        PaperBackupStatusResponse response = PaperBackupStatusResponse.builder()
                .paperId(9L)
                .backupStatus(BackupStatus.BACKED_UP)
                .backupError(null)
                .build();
        when(storageBackupService.restorePaper(9L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/papers/9/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("恢复成功"))
                .andExpect(jsonPath("$.data.paperId").value(9))
                .andExpect(jsonPath("$.data.backupStatus").value("BACKED_UP"));
    }

    @Test
    void restorePaperReturnsBadRequest() throws Exception {
        doThrow(new BusinessException(400, "本地文件已存在，无需恢复"))
                .when(storageBackupService).restorePaper(10L);

        mockMvc.perform(post("/api/v1/papers/10/restore"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("本地文件已存在，无需恢复"));
    }
}
