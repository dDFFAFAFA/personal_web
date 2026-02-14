package com.changye.web.service;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.PaperSummaryGenerateRequest;
import com.changye.web.dto.response.PaperSummaryGenerateResponse;
import com.changye.web.dto.response.PaperSummaryResponse;
import com.changye.web.model.AiProviderConfig;
import com.changye.web.model.Paper;
import com.changye.web.model.PaperSummary;
import com.changye.web.model.enums.AiProvider;
import com.changye.web.model.enums.PaperSummaryStatus;
import com.changye.web.repository.AiProviderConfigRepository;
import com.changye.web.repository.PaperRepository;
import com.changye.web.repository.PaperSummaryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperSummaryServiceTest {

    @Mock
    private PaperRepository paperRepository;

    @Mock
    private PaperSummaryRepository paperSummaryRepository;

    @Mock
    private AiProviderConfigRepository aiProviderConfigRepository;

    private PaperSummaryService paperSummaryService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        paperSummaryService = spy(new PaperSummaryService(
                paperRepository,
                paperSummaryRepository,
                aiProviderConfigRepository,
                new ObjectMapper(),
                new RestTemplateBuilder()
        ));
        ReflectionTestUtils.setField(paperSummaryService, "uploadPath", tempDir.toString());
        lenient().when(paperSummaryRepository.save(any(PaperSummary.class))).thenAnswer(invocation -> {
            PaperSummary summary = invocation.getArgument(0);
            if (summary.getId() == null) {
                summary.setId(100L);
            }
            return summary;
        });
    }

    @Test
    void generateSummarySuccessWithPreferredProvider() throws Exception {
        Path file = Files.createFile(tempDir.resolve("1_paper.pdf"));
        Paper paper = Paper.builder()
                .id(1L)
                .title("Sample Paper")
                .fileStorageKey("1_paper.pdf")
                .build();
        AiProviderConfig deepseek = AiProviderConfig.builder()
                .provider(AiProvider.DEEPSEEK)
                .enabled(true)
                .baseUrl("https://api.deepseek.com/v1")
                .modelName("deepseek-chat")
                .apiKey("sk-test")
                .build();

        when(paperRepository.findById(1L)).thenReturn(Optional.of(paper));
        when(paperSummaryRepository.findByPaperId(1L)).thenReturn(Optional.empty());
        when(aiProviderConfigRepository.findById(AiProvider.DEEPSEEK)).thenReturn(Optional.of(deepseek));
        doReturn("This is pdf content").when(paperSummaryService).extractPdfText(file);
        doReturn("# Structured Summary").when(paperSummaryService).callProvider(any(), any());

        PaperSummaryGenerateRequest request = new PaperSummaryGenerateRequest();
        request.setProvider(AiProvider.DEEPSEEK);
        request.setForceRegenerate(true);
        PaperSummaryGenerateResponse response = paperSummaryService.generateSummary(1L, request);

        assertThat(response.getPaperId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(PaperSummaryStatus.SUCCESS);
        verify(paperSummaryRepository, org.mockito.Mockito.times(2)).save(any(PaperSummary.class));
    }

    @Test
    void generateSummaryFailsWhenNoProviderConfigured() {
        Path file = tempDir.resolve("2_paper.pdf");
        Paper paper = Paper.builder()
                .id(2L)
                .title("No Provider")
                .fileStorageKey("2_paper.pdf")
                .build();
        when(paperRepository.findById(2L)).thenReturn(Optional.of(paper));
        when(paperSummaryRepository.findByPaperId(2L)).thenReturn(Optional.empty());
        when(aiProviderConfigRepository.findById(AiProvider.DEEPSEEK)).thenReturn(Optional.empty());
        when(aiProviderConfigRepository.findById(AiProvider.QWEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paperSummaryService.generateSummary(2L, new PaperSummaryGenerateRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(400);
                    assertThat(businessException.getMessage()).isEqualTo("未找到可用的 AI Provider 配置");
                });
    }

    @Test
    void generateSummarySetsFailedStatusWhenProviderCallFails() throws Exception {
        Path file = Files.createFile(tempDir.resolve("3_paper.pdf"));
        Paper paper = Paper.builder()
                .id(3L)
                .title("Provider Failure")
                .fileStorageKey("3_paper.pdf")
                .build();
        AiProviderConfig qwen = AiProviderConfig.builder()
                .provider(AiProvider.QWEN)
                .enabled(true)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .modelName("qwen-plus")
                .apiKey("sk-test")
                .build();

        when(paperRepository.findById(3L)).thenReturn(Optional.of(paper));
        when(paperSummaryRepository.findByPaperId(3L)).thenReturn(Optional.empty());
        when(aiProviderConfigRepository.findById(AiProvider.QWEN)).thenReturn(Optional.of(qwen));
        doReturn("This is pdf content").when(paperSummaryService).extractPdfText(file);
        doThrow(new BusinessException(502, "AI 服务调用失败")).when(paperSummaryService).callProvider(any(), any());

        PaperSummaryGenerateRequest request = new PaperSummaryGenerateRequest();
        request.setProvider(AiProvider.QWEN);

        assertThatThrownBy(() -> paperSummaryService.generateSummary(3L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(502));
        verify(paperSummaryRepository, org.mockito.Mockito.times(2)).save(any(PaperSummary.class));
    }

    @Test
    void getSummaryReturnsStoredData() {
        Paper paper = Paper.builder().id(4L).title("Summary").build();
        PaperSummary summary = PaperSummary.builder()
                .id(200L)
                .paper(paper)
                .status(PaperSummaryStatus.SUCCESS)
                .provider(AiProvider.DEEPSEEK)
                .modelName("deepseek-chat")
                .markdownContent("# Summary")
                .build();
        when(paperRepository.findById(4L)).thenReturn(Optional.of(paper));
        when(paperSummaryRepository.findByPaperId(4L)).thenReturn(Optional.of(summary));

        PaperSummaryResponse response = paperSummaryService.getSummary(4L);

        assertThat(response.getSummaryId()).isEqualTo(200L);
        assertThat(response.getStatus()).isEqualTo(PaperSummaryStatus.SUCCESS);
        assertThat(response.getMarkdown()).isEqualTo("# Summary");
    }

    @Test
    void listHomeSummariesReturnsEmptyList() {
        when(paperSummaryRepository.findByStatusOrderByGeneratedAtDesc(any(), any())).thenReturn(List.of());

        List<PaperSummaryResponse> response = paperSummaryService.listHomeSummaries(10);

        assertThat(response).isEmpty();
    }


    @Test
    void listHomeSummariesReturnsMappedResultWithExtractedFields() {
        Paper paper = Paper.builder().id(5L).title("Home Summary").build();
        PaperSummary summary = PaperSummary.builder()
                .id(300L)
                .paper(paper)
                .status(PaperSummaryStatus.SUCCESS)
                .provider(AiProvider.QWEN)
                .modelName("qwen-plus")
                .markdownContent("模型名称：Llama-3.1-70B-Instruct\n一句话精要：该方法在公开数据集上显著提升精度。")
                .generatedAt(OffsetDateTime.now())
                .build();
        when(paperSummaryRepository.findByStatusOrderByGeneratedAtDesc(any(), any())).thenReturn(List.of(summary));

        List<PaperSummaryResponse> response = paperSummaryService.listHomeSummaries(10);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getPaperId()).isEqualTo(5L);
        assertThat(response.get(0).getSummaryId()).isEqualTo(300L);
        assertThat(response.get(0).getPaperTitle()).isEqualTo("Home Summary");
        assertThat(response.get(0).getModelName()).isEqualTo("Llama-3.1-70B-Instruct");
        assertThat(response.get(0).getOneSentence()).isEqualTo("该方法在公开数据集上显著提升精度");
    }

    @Test
    void listHomeSummariesFallsBackWhenModelNameMissing() {
        Paper paper = Paper.builder().id(6L).title("No Model Name").build();
        PaperSummary summary = PaperSummary.builder()
                .id(301L)
                .paper(paper)
                .status(PaperSummaryStatus.SUCCESS)
                .provider(AiProvider.DEEPSEEK)
                .modelName("deepseek-chat")
                .markdownContent("该方法通过双塔结构提升召回率。后续通过重排序进一步优化结果。")
                .generatedAt(OffsetDateTime.now())
                .build();
        when(paperSummaryRepository.findByStatusOrderByGeneratedAtDesc(any(), any())).thenReturn(List.of(summary));

        List<PaperSummaryResponse> response = paperSummaryService.listHomeSummaries(10);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getModelName()).isNull();
        assertThat(response.get(0).getOneSentence()).isEqualTo("该方法通过双塔结构提升召回率。");
    }

    @Test
    void listHomeSummariesHandlesEmptyMarkdown() {
        Paper paper = Paper.builder().id(7L).title("Empty Markdown").build();
        PaperSummary summary = PaperSummary.builder()
                .id(302L)
                .paper(paper)
                .status(PaperSummaryStatus.SUCCESS)
                .provider(AiProvider.DEEPSEEK)
                .modelName("deepseek-chat")
                .markdownContent("   ")
                .generatedAt(OffsetDateTime.now())
                .build();
        when(paperSummaryRepository.findByStatusOrderByGeneratedAtDesc(any(), any())).thenReturn(List.of(summary));

        List<PaperSummaryResponse> response = paperSummaryService.listHomeSummaries(10);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getModelName()).isNull();
        assertThat(response.get(0).getOneSentence()).isNull();
    }
}
