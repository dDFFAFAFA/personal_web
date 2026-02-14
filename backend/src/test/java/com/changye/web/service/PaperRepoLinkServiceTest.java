package com.changye.web.service;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.PaperRepoLinksApplyRequest;
import com.changye.web.dto.response.PaperRepoLinkResponse;
import com.changye.web.dto.response.PaperRepoLinksApplyResponse;
import com.changye.web.dto.response.PaperRepoLinksExtractResponse;
import com.changye.web.model.Paper;
import com.changye.web.model.PaperRepoLink;
import com.changye.web.model.enums.PaperRepoLinkProvider;
import com.changye.web.model.enums.PaperRepoLinkStatus;
import com.changye.web.repository.PaperCodeEntryRepository;
import com.changye.web.repository.PaperRepoLinkRepository;
import com.changye.web.repository.PaperRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperRepoLinkServiceTest {

    @Mock
    private PaperRepository paperRepository;

    @Mock
    private PaperRepoLinkRepository paperRepoLinkRepository;

    @Mock
    private PaperCodeEntryRepository paperCodeEntryRepository;

    @Mock
    private RepoReadmeService repoReadmeService;

    @Mock
    private PaperSummaryService paperSummaryService;

    private PaperRepoLinkService paperRepoLinkService;

    @BeforeEach
    void setUp() {
        paperRepoLinkService = new PaperRepoLinkService(
                paperRepository,
                paperRepoLinkRepository,
                paperCodeEntryRepository,
                repoReadmeService,
                paperSummaryService
        );
    }

    @Test
    void extractLinksDeduplicatesGithubAndGitee() {
        Paper paper = Paper.builder().id(1L).title("Paper").build();
        when(paperRepository.findById(1L)).thenReturn(Optional.of(paper));
        when(paperSummaryService.resolvePaperFilePath(paper)).thenReturn(Path.of("/tmp/paper.pdf"));
        when(paperSummaryService.extractPdfText(Path.of("/tmp/paper.pdf"))).thenReturn("""
                code: https://github.com/foo/bar.
                mirror: https://gitee.com/foo/bar
                duplicate: https://github.com/foo/bar
                """);
        when(paperRepoLinkRepository.findByPaperIdAndUrl(1L, "https://github.com/foo/bar")).thenReturn(Optional.empty());
        when(paperRepoLinkRepository.findByPaperIdAndUrl(1L, "https://gitee.com/foo/bar")).thenReturn(Optional.empty());

        PaperRepoLink github = PaperRepoLink.builder()
                .id(10L)
                .paper(paper)
                .url("https://github.com/foo/bar")
                .provider(PaperRepoLinkProvider.GITHUB)
                .status(PaperRepoLinkStatus.CANDIDATE)
                .build();
        PaperRepoLink gitee = PaperRepoLink.builder()
                .id(11L)
                .paper(paper)
                .url("https://gitee.com/foo/bar")
                .provider(PaperRepoLinkProvider.GITEE)
                .status(PaperRepoLinkStatus.CANDIDATE)
                .build();
        when(paperRepoLinkRepository.findByPaperIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(github, gitee));

        PaperRepoLinksExtractResponse response = paperRepoLinkService.extractLinks(1L);

        assertThat(response.getPaperId()).isEqualTo(1L);
        assertThat(response.getCandidateCount()).isEqualTo(2);
        assertThat(response.getCandidates()).extracting(PaperRepoLinkResponse::getUrl)
                .containsExactlyInAnyOrder("https://github.com/foo/bar", "https://gitee.com/foo/bar");
        verify(paperRepoLinkRepository).saveAll(any());
    }

    @Test
    void applyLinksCreatesCodeEntriesAndRebuildsReadme() {
        Paper paper = Paper.builder().id(2L).title("Paper").build();
        PaperRepoLink link = PaperRepoLink.builder()
                .id(20L)
                .paper(paper)
                .url("https://github.com/foo/bar")
                .provider(PaperRepoLinkProvider.GITHUB)
                .status(PaperRepoLinkStatus.CANDIDATE)
                .build();
        when(paperRepository.findById(2L)).thenReturn(Optional.of(paper));
        when(paperRepoLinkRepository.findByPaperIdAndIdIn(2L, List.of(20L))).thenReturn(List.of(link));
        when(paperCodeEntryRepository.existsByPaperIdAndRepoUrl(2L, "https://github.com/foo/bar")).thenReturn(false);

        PaperRepoLinksApplyRequest request = new PaperRepoLinksApplyRequest();
        request.setCandidateIds(List.of(20L));
        request.setAutoRebuildReadme(true);
        PaperRepoLinksApplyResponse response = paperRepoLinkService.applyLinks(2L, request);

        assertThat(response.getAppliedCount()).isEqualTo(1);
        assertThat(link.getStatus()).isEqualTo(PaperRepoLinkStatus.APPLIED);
        verify(paperCodeEntryRepository).save(any());
        verify(repoReadmeService).rebuildReadme(false);
    }

    @Test
    void applyLinksReturnsBadRequestWhenNoCandidates() {
        Paper paper = Paper.builder().id(3L).title("Paper").build();
        when(paperRepository.findById(3L)).thenReturn(Optional.of(paper));
        when(paperRepoLinkRepository.findByPaperIdAndStatusOrderByUpdatedAtDesc(3L, PaperRepoLinkStatus.CANDIDATE))
                .thenReturn(List.of());

        assertThatThrownBy(() -> paperRepoLinkService.applyLinks(3L, new PaperRepoLinksApplyRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(400);
                    assertThat(businessException.getMessage()).isEqualTo("没有可应用的链接");
                });
        verify(paperCodeEntryRepository, never()).save(any());
    }
}
