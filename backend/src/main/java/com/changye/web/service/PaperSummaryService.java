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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Transactional
public class PaperSummaryService {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_PROMPT_TEXT_LENGTH = 18_000;
    private static final Pattern MODEL_NAME_PATTERN = Pattern.compile("(?:模型名称|模型名|模型)\\s*[：:]\\s*(.+)");
    private static final Pattern ONE_SENTENCE_PATTERN = Pattern.compile("(?:一句话精要|一句话总结|一句话概括|一句话摘要)\\s*[：:]\\s*(.+)");

    private final PaperRepository paperRepository;
    private final PaperSummaryRepository paperSummaryRepository;
    private final AiProviderConfigRepository aiProviderConfigRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${app.upload.path}")
    private String uploadPath;

    public PaperSummaryService(PaperRepository paperRepository,
                               PaperSummaryRepository paperSummaryRepository,
                               AiProviderConfigRepository aiProviderConfigRepository,
                               ObjectMapper objectMapper,
                               RestTemplateBuilder restTemplateBuilder) {
        this.paperRepository = paperRepository;
        this.paperSummaryRepository = paperSummaryRepository;
        this.aiProviderConfigRepository = aiProviderConfigRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(TIMEOUT)
                .setReadTimeout(TIMEOUT)
                .build();
    }

    public PaperSummaryGenerateResponse generateSummary(Long paperId, PaperSummaryGenerateRequest request) {
        Paper paper = findPaper(paperId);
        PaperSummary summary = paperSummaryRepository.findByPaperId(paperId)
                .orElseGet(() -> PaperSummary.builder()
                        .paper(paper)
                        .status(PaperSummaryStatus.PENDING)
                        .build());

        boolean forceRegenerate = request != null && Boolean.TRUE.equals(request.getForceRegenerate());
        if (!forceRegenerate
                && summary.getStatus() == PaperSummaryStatus.SUCCESS
                && StringUtils.hasText(summary.getMarkdownContent())) {
            return PaperSummaryGenerateResponse.builder()
                    .paperId(paperId)
                    .summaryId(summary.getId())
                    .status(PaperSummaryStatus.SUCCESS)
                    .message("摘要已存在")
                    .build();
        }

        AiProviderConfig providerConfig = resolveProviderConfig(request == null ? null : request.getProvider());
        summary.setStatus(PaperSummaryStatus.GENERATING);
        summary.setProvider(providerConfig.getProvider());
        summary.setModelName(providerConfig.getModelName());
        summary.setErrorMessage(null);
        paperSummaryRepository.save(summary);

        long startedAt = System.nanoTime();
        try {
            String pdfText = extractPdfText(resolvePaperFilePath(paper));
            String markdown = callProvider(providerConfig, buildPrompt(paper, pdfText));
            String oneSentence = extractOneSentenceFromMarkdown(markdown);
            summary.setStatus(PaperSummaryStatus.SUCCESS);
            summary.setMarkdownContent(markdown);
            summary.setOneSentence(oneSentence);
            summary.setErrorMessage(null);
            summary.setGeneratedAt(OffsetDateTime.now());
            PaperSummary saved = paperSummaryRepository.save(summary);
            log.info("paper_summary_generate paperId={} status=SUCCESS provider={} durationMs={}",
                    paperId, providerConfig.getProvider(), elapsedMs(startedAt));
            return PaperSummaryGenerateResponse.builder()
                    .paperId(paperId)
                    .summaryId(saved.getId())
                    .status(saved.getStatus())
                    .message("摘要生成成功")
                    .build();
        } catch (BusinessException ex) {
            summary.setStatus(PaperSummaryStatus.FAILED);
            summary.setErrorMessage(trimError(ex.getMessage()));
            paperSummaryRepository.save(summary);
            log.warn("paper_summary_generate paperId={} status=FAILED provider={} durationMs={} reason={}",
                    paperId, providerConfig.getProvider(), elapsedMs(startedAt), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            summary.setStatus(PaperSummaryStatus.FAILED);
            summary.setErrorMessage("AI 服务调用失败");
            paperSummaryRepository.save(summary);
            log.warn("paper_summary_generate paperId={} status=FAILED provider={} durationMs={} reason={}",
                    paperId, providerConfig.getProvider(), elapsedMs(startedAt), ex.getMessage());
            throw new BusinessException(502, "AI 服务调用失败");
        }
    }

    @Transactional(readOnly = true)
    public PaperSummaryResponse getSummary(Long paperId) {
        findPaper(paperId);
        PaperSummary summary = paperSummaryRepository.findByPaperId(paperId)
                .orElseThrow(() -> new BusinessException(404, "摘要不存在"));
        return toResponse(summary);
    }

    @Transactional(readOnly = true)
    public String getSummaryMarkdown(Long paperId) {
        PaperSummaryResponse summary = getSummary(paperId);
        if (summary.getStatus() != PaperSummaryStatus.SUCCESS || !StringUtils.hasText(summary.getMarkdown())) {
            throw new BusinessException(404, "摘要尚未生成成功");
        }
        return summary.getMarkdown();
    }

    @Transactional(readOnly = true)
    public List<PaperSummaryResponse> listHomeSummaries(Integer limit) {
        Pageable pageable = PageRequest.of(0, normalizeLimit(limit));
        return paperSummaryRepository
                .findByStatusOrderByGeneratedAtDesc(PaperSummaryStatus.SUCCESS, pageable)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Paper findPaper(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));
    }

    private AiProviderConfig resolveProviderConfig(AiProvider preferredProvider) {
        if (preferredProvider != null) {
            return getEnabledConfig(preferredProvider);
        }
        List<AiProvider> providers = List.of(AiProvider.DEEPSEEK, AiProvider.QWEN);
        for (AiProvider provider : providers) {
            AiProviderConfig config = aiProviderConfigRepository.findById(provider).orElse(null);
            if (config != null && isConfigEnabled(config)) {
                return config;
            }
        }
        throw new BusinessException(400, "未找到可用的 AI Provider 配置");
    }

    private AiProviderConfig getEnabledConfig(AiProvider provider) {
        AiProviderConfig config = aiProviderConfigRepository.findById(provider)
                .orElseThrow(() -> new BusinessException(400, "AI Provider 未配置: " + provider));
        if (!isConfigEnabled(config)) {
            throw new BusinessException(400, "AI Provider 不可用: " + provider);
        }
        return config;
    }

    private boolean isConfigEnabled(AiProviderConfig config) {
        return config.getEnabled() != null
                && config.getEnabled()
                && StringUtils.hasText(config.getBaseUrl())
                && StringUtils.hasText(config.getModelName())
                && StringUtils.hasText(config.getApiKey());
    }

    String callProvider(AiProviderConfig config, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());

        String body = createRequestBody(config.getModelName(), prompt);
        String endpoint = config.getBaseUrl().trim();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        endpoint = endpoint + "/chat/completions";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            String content = parseCompletionContent(response.getBody());
            if (!StringUtils.hasText(content)) {
                throw new BusinessException(502, "AI 返回内容为空");
            }
            return content.trim();
        } catch (RestClientException ex) {
            throw new BusinessException(502, "AI 服务调用失败");
        }
    }

    private String createRequestBody(String model, String prompt) {
        try {
            return objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("model", model)
                            .put("temperature", 0.2)
                            .set("messages", objectMapper.createArrayNode()
                                    .add(objectMapper.createObjectNode()
                                            .put("role", "system")
                                            .put("content", "你是科研论文助手。请输出结构化中文 Markdown 摘要，必须包含两行字段：模型名称：...；一句话精要：...。并包含：问题定义、核心方法、实验设置、关键结果、局限性、可复现要点。"))
                                    .add(objectMapper.createObjectNode()
                                            .put("role", "user")
                                            .put("content", prompt)))
            );
        } catch (Exception ex) {
            throw new BusinessException(500, "摘要请求构建失败");
        }
    }

    private String parseCompletionContent(String rawBody) {
        if (!StringUtils.hasText(rawBody)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception ex) {
            throw new BusinessException(502, "AI 响应解析失败");
        }
    }

    Path resolvePaperFilePath(Paper paper) {
        if (StringUtils.hasText(paper.getFileStorageKey())) {
            Path base = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path resolved = base.resolve(normalizeStorageKey(paper.getFileStorageKey())).normalize();
            if (!resolved.startsWith(base)) {
                throw new BusinessException(400, "论文文件路径非法");
            }
            ensureReadable(resolved);
            return resolved;
        }
        if (!StringUtils.hasText(paper.getFilePath())) {
            throw new BusinessException(404, "论文文件不存在");
        }
        Path path = Paths.get(paper.getFilePath()).toAbsolutePath().normalize();
        ensureReadable(path);
        return path;
    }

    private String normalizeStorageKey(String storageKey) {
        String normalized = storageKey.replace("\\", "/");
        if (normalized.startsWith("/") || normalized.contains("..")) {
            throw new BusinessException(400, "论文文件路径非法");
        }
        return normalized;
    }

    String extractPdfText(Path pdfPath) {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String raw = stripper.getText(document);
            if (!StringUtils.hasText(raw)) {
                throw new BusinessException(400, "PDF 文本为空，无法生成摘要");
            }
            return raw;
        } catch (IOException ex) {
            throw new BusinessException(500, "PDF 读取失败");
        }
    }

    private void ensureReadable(Path path) {
        if (!Files.exists(path)) {
            throw new BusinessException(404, "论文文件不存在");
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new BusinessException(400, "论文文件不可读");
        }
    }

    private String buildPrompt(Paper paper, String pdfText) {
        String text = pdfText.length() > MAX_PROMPT_TEXT_LENGTH
                ? pdfText.substring(0, MAX_PROMPT_TEXT_LENGTH)
                : pdfText;
        return """
                论文标题：%s

                请基于以下论文正文文本输出结构化 Markdown 摘要：
                %s
                """.formatted(paper.getTitle(), text);
    }



    private PaperSummaryResponse toResponse(PaperSummary summary) {
        String markdown = summary.getMarkdownContent();
        return PaperSummaryResponse.builder()
                .summaryId(summary.getId())
                .paperId(summary.getPaper().getId())
                .paperTitle(summary.getPaper().getTitle())
                .status(summary.getStatus())
                .provider(summary.getProvider())
                .model(summary.getModelName())
                .modelName(extractModelNameFromMarkdown(markdown))
                .oneSentence(normalizeOneSentence(summary.getOneSentence()))
                .markdown(markdown)
                .generatedAt(summary.getGeneratedAt())
                .error(summary.getErrorMessage())
                .build();
    }

    private String extractModelNameFromMarkdown(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }
        for (String line : markdown.split("\\R")) {
            String cleaned = normalizeLine(line);
            if (!StringUtils.hasText(cleaned)) {
                continue;
            }
            Matcher matcher = MODEL_NAME_PATTERN.matcher(cleaned);
            if (matcher.find()) {
                String value = cleanupExtractedField(matcher.group(1));
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return "";
    }

    private String extractOneSentenceFromMarkdown(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }
        for (String line : markdown.split("\\R")) {
            String cleaned = normalizeLine(line);
            if (!StringUtils.hasText(cleaned)) {
                continue;
            }
            Matcher matcher = ONE_SENTENCE_PATTERN.matcher(cleaned);
            if (matcher.find()) {
                String value = cleanupExtractedField(matcher.group(1));
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return "";
    }

    private String normalizeLine(String line) {
        return line == null ? "" : line
                .replaceAll("^\\s*[#>*-]+\\s*", "")
                .replace("`", "")
                .trim();
    }

    private String cleanupExtractedField(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value
                .replaceAll("\\[(.*?)\\]\\((.*?)\\)", "$1")
                .replaceAll("[*_~`]+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeOneSentence(String oneSentence) {
        return StringUtils.hasText(oneSentence) ? oneSentence.trim() : "";
    }
    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 10;
        }
        if (limit < 1) {
            return 1;
        }
        if (limit > 50) {
            return 50;
        }
        return limit;
    }

    private String trimError(String message) {
        if (!StringUtils.hasText(message)) {
            return "UNKNOWN";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
