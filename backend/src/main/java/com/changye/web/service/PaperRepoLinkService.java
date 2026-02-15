package com.changye.web.service;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.PaperRepoLinksApplyRequest;
import com.changye.web.dto.response.PaperRepoLinkResponse;
import com.changye.web.dto.response.PaperRepoLinksApplyResponse;
import com.changye.web.dto.response.PaperRepoLinksExtractResponse;
import com.changye.web.model.Paper;
import com.changye.web.model.PaperCodeEntry;
import com.changye.web.model.PaperRepoLink;
import com.changye.web.model.enums.PaperRepoLinkProvider;
import com.changye.web.model.enums.PaperRepoLinkStatus;
import com.changye.web.model.enums.RepoProvider;
import com.changye.web.repository.PaperCodeEntryRepository;
import com.changye.web.repository.PaperRepoLinkRepository;
import com.changye.web.repository.PaperRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@Transactional
public class PaperRepoLinkService {

    private static final Pattern REPO_LINK_PATTERN = Pattern.compile(
            "(https?://(?:www\\.)?(?:github\\.com|gitee\\.com)/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.\\-/%]+)?)",
            Pattern.CASE_INSENSITIVE
    );

    private final PaperRepository paperRepository;
    private final PaperRepoLinkRepository paperRepoLinkRepository;
    private final PaperCodeEntryRepository paperCodeEntryRepository;
    private final RepoReadmeService repoReadmeService;
    private final PaperSummaryService paperSummaryService;

    public PaperRepoLinkService(PaperRepository paperRepository,
                                PaperRepoLinkRepository paperRepoLinkRepository,
                                PaperCodeEntryRepository paperCodeEntryRepository,
                                RepoReadmeService repoReadmeService,
                                PaperSummaryService paperSummaryService) {
        this.paperRepository = paperRepository;
        this.paperRepoLinkRepository = paperRepoLinkRepository;
        this.paperCodeEntryRepository = paperCodeEntryRepository;
        this.repoReadmeService = repoReadmeService;
        this.paperSummaryService = paperSummaryService;
    }

    public PaperRepoLinksExtractResponse extractLinks(Long paperId) {
        Paper paper = findPaper(paperId);
        Path filePath = paperSummaryService.resolvePaperFilePath(paper);
        String pdfText = paperSummaryService.extractPdfText(filePath);

        Set<String> dedupedUrls = extractUrls(pdfText);
        List<PaperRepoLink> toSave = new ArrayList<>();
        for (String url : dedupedUrls) {
            if (paperRepoLinkRepository.findByPaperIdAndUrl(paperId, url).isPresent()) {
                continue;
            }
            toSave.add(PaperRepoLink.builder()
                    .paper(paper)
                    .url(url)
                    .provider(resolveProvider(url))
                    .status(PaperRepoLinkStatus.CANDIDATE)
                    .sourceText(url)
                    .confidence(BigDecimal.valueOf(0.9))
                    .build());
        }
        if (!toSave.isEmpty()) {
            paperRepoLinkRepository.saveAll(toSave);
        }

        List<PaperRepoLinkResponse> candidates = listLinks(paperId);
        long candidateCount = candidates.stream()
                .filter(candidate -> candidate.getStatus() == PaperRepoLinkStatus.CANDIDATE)
                .count();
        log.info("paper_repo_links_extract paperId={} candidateCount={}", paperId, candidateCount);
        return PaperRepoLinksExtractResponse.builder()
                .paperId(paperId)
                .candidateCount(Math.toIntExact(candidateCount))
                .candidates(candidates)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PaperRepoLinkResponse> listLinks(Long paperId) {
        findPaper(paperId);
        return paperRepoLinkRepository.findByPaperIdOrderByUpdatedAtDesc(paperId).stream()
                .map(this::toResponse)
                .toList();
    }

    public PaperRepoLinksApplyResponse applyLinks(Long paperId, PaperRepoLinksApplyRequest request) {
        Paper paper = findPaper(paperId);
        boolean autoRebuild = request == null || request.getAutoRebuildReadme() == null
                || request.getAutoRebuildReadme();
        List<PaperRepoLink> targets = resolveApplyTargets(paperId, request);
        if (targets.isEmpty()) {
            throw new BusinessException(400, "没有可应用的链接");
        }

        List<Long> appliedIds = new ArrayList<>();
        for (PaperRepoLink link : targets) {
            if (link.getProvider() == PaperRepoLinkProvider.UNKNOWN) {
                continue;
            }
            if (!paperCodeEntryRepository.existsByPaperIdAndRepoUrl(paperId, link.getUrl())) {
                paperCodeEntryRepository.save(PaperCodeEntry.builder()
                        .paper(paper)
                        .repoUrl(link.getUrl())
                        .branchName("main")
                        .provider(toRepoProvider(link.getProvider()))
                        .description("Auto extracted from PDF")
                        .build());
            }
            link.setStatus(PaperRepoLinkStatus.APPLIED);
            appliedIds.add(link.getId());
        }
        paperRepoLinkRepository.saveAll(targets);
        if (autoRebuild && !appliedIds.isEmpty()) {
            rebuildReadmeQuietly();
        }

        log.info("paper_repo_links_apply paperId={} appliedCount={} autoRebuildReadme={}",
                paperId, appliedIds.size(), autoRebuild);
        return PaperRepoLinksApplyResponse.builder()
                .paperId(paperId)
                .appliedCount(appliedIds.size())
                .appliedCandidateIds(appliedIds)
                .build();
    }

    public PaperRepoLinkResponse rejectLink(Long paperId, Long candidateId) {
        findPaper(paperId);
        PaperRepoLink link = paperRepoLinkRepository.findByPaperIdAndId(paperId, candidateId)
                .orElseThrow(() -> new BusinessException(404, "候选链接不存在"));
        if (link.getStatus() == PaperRepoLinkStatus.APPLIED) {
            throw new BusinessException(400, "已应用链接不可拒绝");
        }
        if (link.getStatus() != PaperRepoLinkStatus.REJECTED) {
            link.setStatus(PaperRepoLinkStatus.REJECTED);
            paperRepoLinkRepository.save(link);
        }
        return toResponse(link);
    }

    private List<PaperRepoLink> resolveApplyTargets(Long paperId, PaperRepoLinksApplyRequest request) {
        if (request != null && request.getCandidateIds() != null && !request.getCandidateIds().isEmpty()) {
            return paperRepoLinkRepository.findByPaperIdAndIdIn(paperId, request.getCandidateIds());
        }
        return paperRepoLinkRepository.findByPaperIdAndStatusOrderByUpdatedAtDesc(paperId, PaperRepoLinkStatus.CANDIDATE);
    }

    private void rebuildReadmeQuietly() {
        try {
            repoReadmeService.rebuildReadme(false);
        } catch (Exception ex) {
            log.warn("README auto rebuild skipped: {}", ex.getMessage());
        }
    }

    private Paper findPaper(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));
    }

    private Set<String> extractUrls(String pdfText) {
        Set<String> urls = new LinkedHashSet<>();
        Matcher matcher = REPO_LINK_PATTERN.matcher(pdfText);
        while (matcher.find()) {
            String normalized = normalizeUrl(matcher.group(1));
            if (StringUtils.hasText(normalized)) {
                urls.add(normalized);
            }
        }
        return urls;
    }

    private String normalizeUrl(String url) {
        String normalized = url == null ? "" : url.trim();
        while (normalized.endsWith(".")
                || normalized.endsWith(",")
                || normalized.endsWith(";")
                || normalized.endsWith(")")
                || normalized.endsWith("]")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private PaperRepoLinkProvider resolveProvider(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("github.com")) {
            return PaperRepoLinkProvider.GITHUB;
        }
        if (lower.contains("gitee.com")) {
            return PaperRepoLinkProvider.GITEE;
        }
        return PaperRepoLinkProvider.UNKNOWN;
    }

    private RepoProvider toRepoProvider(PaperRepoLinkProvider provider) {
        return switch (provider) {
            case GITHUB -> RepoProvider.GITHUB;
            case GITEE -> RepoProvider.GITEE;
            default -> throw new BusinessException(400, "不支持的仓库提供方");
        };
    }

    private PaperRepoLinkResponse toResponse(PaperRepoLink link) {
        return PaperRepoLinkResponse.builder()
                .id(link.getId())
                .paperId(link.getPaper().getId())
                .url(link.getUrl())
                .provider(link.getProvider())
                .status(link.getStatus())
                .sourceText(link.getSourceText())
                .pageNo(link.getPageNo())
                .confidence(link.getConfidence())
                .createdAt(link.getCreatedAt())
                .updatedAt(link.getUpdatedAt())
                .build();
    }
}
