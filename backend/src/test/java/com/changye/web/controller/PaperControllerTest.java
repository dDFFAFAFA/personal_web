package com.changye.web.controller;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.PaperRepoLinksApplyRequest;
import com.changye.web.dto.request.PaperCreateRequest;
import com.changye.web.dto.request.PaperSummaryGenerateRequest;
import com.changye.web.dto.response.PaperBackupStatusResponse;
import com.changye.web.dto.response.PaperRepoLinkResponse;
import com.changye.web.dto.response.PaperRepoLinksApplyResponse;
import com.changye.web.dto.response.PaperRepoLinksExtractResponse;
import com.changye.web.dto.response.PaperResponse;
import com.changye.web.dto.response.PaperSummaryGenerateResponse;
import com.changye.web.dto.response.PaperSummaryResponse;
import com.changye.web.model.enums.AiProvider;
import com.changye.web.model.enums.BackupStatus;
import com.changye.web.model.enums.PaperRepoLinkProvider;
import com.changye.web.model.enums.PaperRepoLinkStatus;
import com.changye.web.model.enums.PaperSummaryStatus;
import com.changye.web.model.enums.ReadingStatus;
import com.changye.web.service.PaperRepoLinkService;
import com.changye.web.service.PaperService;
import com.changye.web.service.PaperSummaryService;
import com.changye.web.service.StorageBackupService;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaperController.class)
class PaperControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaperService paperService;

    @MockBean
    private StorageBackupService storageBackupService;

    @MockBean
    private PaperSummaryService paperSummaryService;

    @MockBean
    private PaperRepoLinkService paperRepoLinkService;

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
    void listHomeSummariesReturnsEmptyList() throws Exception {
        when(paperSummaryService.listHomeSummaries(10)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/home/summaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
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
    @Test
    void generateSummaryReturnsSuccess() throws Exception {
        PaperSummaryGenerateResponse response = PaperSummaryGenerateResponse.builder()
                .paperId(11L)
                .summaryId(100L)
                .status(PaperSummaryStatus.SUCCESS)
                .message("摘要生成成功")
                .build();
        when(paperSummaryService.generateSummary(any(), any())).thenReturn(response);

        PaperSummaryGenerateRequest request = new PaperSummaryGenerateRequest();
        request.setProvider(AiProvider.DEEPSEEK);
        request.setForceRegenerate(true);

        mockMvc.perform(post("/api/v1/papers/11/summary/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.paperId").value(11))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void generateSummaryReturnsError() throws Exception {
        doThrow(new BusinessException(400, "未找到可用的 AI Provider 配置"))
                .when(paperSummaryService).generateSummary(any(), any());

        mockMvc.perform(post("/api/v1/papers/11/summary/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("未找到可用的 AI Provider 配置"));
    }

    @Test
    void getSummaryReturnsSuccess() throws Exception {
        PaperSummaryResponse response = PaperSummaryResponse.builder()
                .summaryId(101L)
                .paperId(12L)
                .status(PaperSummaryStatus.SUCCESS)
                .provider(AiProvider.DEEPSEEK)
                .model("deepseek-chat")
                .markdown("# Summary")
                .build();
        when(paperSummaryService.getSummary(12L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/papers/12/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.paperId").value(12))
                .andExpect(jsonPath("$.data.provider").value("DEEPSEEK"));
    }

    @Test
    void downloadSummaryReturnsMarkdown() throws Exception {
        when(paperSummaryService.getSummaryMarkdown(12L)).thenReturn("# Summary");

        mockMvc.perform(get("/api/v1/papers/12/summary/download"))
                .andExpect(status().isOk())
                .andExpect(content().string("# Summary"));
    }

    @Test
    void downloadSummaryReturnsNotFound() throws Exception {
        doThrow(new BusinessException(404, "摘要尚未生成成功"))
                .when(paperSummaryService).getSummaryMarkdown(12L);

        mockMvc.perform(get("/api/v1/papers/12/summary/download"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("摘要尚未生成成功"));
    }

    @Test
    void extractRepoLinksReturnsSuccess() throws Exception {
        PaperRepoLinkResponse candidate = PaperRepoLinkResponse.builder()
                .id(1L)
                .paperId(13L)
                .url("https://github.com/foo/bar")
                .provider(PaperRepoLinkProvider.GITHUB)
                .status(PaperRepoLinkStatus.CANDIDATE)
                .build();
        PaperRepoLinksExtractResponse response = PaperRepoLinksExtractResponse.builder()
                .paperId(13L)
                .candidateCount(1)
                .candidates(List.of(candidate))
                .build();
        when(paperRepoLinkService.extractLinks(13L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/papers/13/repo-links/extract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.paperId").value(13))
                .andExpect(jsonPath("$.data.candidateCount").value(1));
    }

    @Test
    void listRepoLinksReturnsSuccess() throws Exception {
        PaperRepoLinkResponse candidate = PaperRepoLinkResponse.builder()
                .id(2L)
                .paperId(14L)
                .url("https://gitee.com/foo/bar")
                .provider(PaperRepoLinkProvider.GITEE)
                .status(PaperRepoLinkStatus.CANDIDATE)
                .build();
        when(paperRepoLinkService.listLinks(14L)).thenReturn(List.of(candidate));

        mockMvc.perform(get("/api/v1/papers/14/repo-links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].url").value("https://gitee.com/foo/bar"))
                .andExpect(jsonPath("$.data[0].provider").value("GITEE"));
    }

    @Test
    void applyRepoLinksReturnsSuccess() throws Exception {
        PaperRepoLinksApplyRequest request = new PaperRepoLinksApplyRequest();
        request.setCandidateIds(List.of(1L, 2L));
        request.setAutoRebuildReadme(true);

        PaperRepoLinksApplyResponse response = PaperRepoLinksApplyResponse.builder()
                .paperId(15L)
                .appliedCount(2)
                .appliedCandidateIds(List.of(1L, 2L))
                .build();
        when(paperRepoLinkService.applyLinks(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/papers/15/repo-links/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("应用成功"))
                .andExpect(jsonPath("$.data.appliedCount").value(2));
    }

    @Test
    void applyRepoLinksReturnsBadRequest() throws Exception {
        doThrow(new BusinessException(400, "没有可应用的链接"))
                .when(paperRepoLinkService).applyLinks(any(), any());

        mockMvc.perform(post("/api/v1/papers/15/repo-links/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("没有可应用的链接"));
    }
}
