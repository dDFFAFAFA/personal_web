package com.changye.web.service;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.PaperCodeEntryRequest;
import com.changye.web.dto.response.PaperCodeEntryResponse;
import com.changye.web.model.Paper;
import com.changye.web.model.PaperCodeEntry;
import com.changye.web.repository.PaperCodeEntryRepository;
import com.changye.web.repository.PaperRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@Transactional
public class PaperCodeEntryService {

    private final PaperCodeEntryRepository paperCodeEntryRepository;
    private final PaperRepository paperRepository;
    private final RepoReadmeService repoReadmeService;

    public PaperCodeEntryService(PaperCodeEntryRepository paperCodeEntryRepository,
                                 PaperRepository paperRepository,
                                 RepoReadmeService repoReadmeService) {
        this.paperCodeEntryRepository = paperCodeEntryRepository;
        this.paperRepository = paperRepository;
        this.repoReadmeService = repoReadmeService;
    }

    @Transactional(readOnly = true)
    public List<PaperCodeEntryResponse> listEntries() {
        return paperCodeEntryRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaperCodeEntryResponse> listEntriesByPaper(Long paperId) {
        return paperCodeEntryRepository.findByPaperIdOrderByUpdatedAtDesc(paperId).stream()
                .map(this::toResponse)
                .toList();
    }

    public PaperCodeEntryResponse createEntry(PaperCodeEntryRequest request) {
        Paper paper = findPaper(request.getPaperId());
        PaperCodeEntry entry = PaperCodeEntry.builder()
                .paper(paper)
                .repoUrl(request.getRepoUrl().trim())
                .branchName(resolveBranch(request.getBranch()))
                .provider(request.getProvider())
                .description(request.getDescription())
                .build();
        PaperCodeEntry saved = paperCodeEntryRepository.save(entry);
        rebuildReadmeQuietly();
        return toResponse(saved);
    }

    public PaperCodeEntryResponse updateEntry(Long id, PaperCodeEntryRequest request) {
        PaperCodeEntry entry = paperCodeEntryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "代码条目不存在"));

        Paper paper = findPaper(request.getPaperId());
        entry.setPaper(paper);
        entry.setRepoUrl(request.getRepoUrl().trim());
        entry.setBranchName(resolveBranch(request.getBranch()));
        entry.setProvider(request.getProvider());
        entry.setDescription(request.getDescription());

        PaperCodeEntry saved = paperCodeEntryRepository.save(entry);
        rebuildReadmeQuietly();
        return toResponse(saved);
    }

    public void deleteEntry(Long id) {
        PaperCodeEntry entry = paperCodeEntryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "代码条目不存在"));
        paperCodeEntryRepository.delete(entry);
        rebuildReadmeQuietly();
    }

    public void rebuildReadme() {
        repoReadmeService.rebuildReadme(true);
    }

    private Paper findPaper(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));
    }

    private String resolveBranch(String branch) {
        return StringUtils.hasText(branch) ? branch.trim() : "main";
    }

    private void rebuildReadmeQuietly() {
        try {
            repoReadmeService.rebuildReadme(false);
        } catch (Exception ex) {
            log.warn("README auto rebuild skipped: {}", ex.getMessage());
        }
    }

    private PaperCodeEntryResponse toResponse(PaperCodeEntry entry) {
        return PaperCodeEntryResponse.builder()
                .id(entry.getId())
                .paperId(entry.getPaper().getId())
                .paperTitle(entry.getPaper().getTitle())
                .repoUrl(entry.getRepoUrl())
                .branch(entry.getBranchName())
                .provider(entry.getProvider())
                .description(entry.getDescription())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
