package com.changye.web.service;

import com.changye.web.model.PaperCodeEntry;
import com.changye.web.repository.PaperCodeEntryRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepoEntrySyncServiceTest {

    @Mock
    private PaperCodeEntryRepository paperCodeEntryRepository;

    @TempDir
    Path tempDir;

    private StubRepoEntrySyncService repoEntrySyncService;

    @BeforeEach
    void setUp() {
        repoEntrySyncService = new StubRepoEntrySyncService(paperCodeEntryRepository);
        ReflectionTestUtils.setField(repoEntrySyncService, "repoBaseWorkdir", tempDir.toString());
        ReflectionTestUtils.setField(repoEntrySyncService, "sshPrivateKeyPath", "/etc/personal-web/keys/id_ed25519");
        ReflectionTestUtils.setField(repoEntrySyncService, "commandTimeoutSeconds", 5L);
    }

    @Test
    void extractRepoNameSupportsSshAndHttps() {
        assertThat(repoEntrySyncService.extractRepoName("git@github.com:openai/codex.git")).isEqualTo("codex");
        assertThat(repoEntrySyncService.extractRepoName("ssh://git@gitee.com/foo/bar.git")).isEqualTo("bar");
        assertThat(repoEntrySyncService.extractRepoName("https://github.com/foo/personal_web")).isEqualTo("personal_web");

        assertThatThrownBy(() -> repoEntrySyncService.extractRepoName("https://github.com/foo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("格式非法");
    }

    @Test
    void pullFallsBackToDefaultBranchWhenRequestedBranchMissing() throws Exception {
        Path localRepo = tempDir.resolve("paper-code").resolve("my-repo");
        Files.createDirectories(localRepo);
        Files.createFile(localRepo.resolve(".git"));

        PaperCodeEntry entry = PaperCodeEntry.builder()
                .id(1L)
                .repoUrl("https://github.com/foo/my-repo.git")
                .branchName("feature-x")
                .build();
        when(paperCodeEntryRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(entry));

        repoEntrySyncService.enqueue(0, "ok"); // rev-parse --verify HEAD
        repoEntrySyncService.enqueue(1, "");   // local branch missing
        repoEntrySyncService.enqueue(1, "");   // remote branch missing
        repoEntrySyncService.enqueue(0, "origin/main\n");
        repoEntrySyncService.enqueue(0, "");   // checkout main
        repoEntrySyncService.enqueue(0, "Already up to date");

        List<RepoEntrySyncResult> results = repoEntrySyncService.syncEntries(null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAction()).isEqualTo("PULL");
        assertThat(results.get(0).getMessage()).contains("回退默认分支 main");
        assertThat(repoEntrySyncService.commands()).anyMatch(cmd -> cmd.contains("checkout main"));
        assertThat(repoEntrySyncService.commands()).anyMatch(cmd -> cmd.contains("pull origin main"));
    }

    @Test
    void emptyRepositoryReturnsSkipStatus() throws Exception {
        Path localRepo = tempDir.resolve("paper-code").resolve("empty-repo");
        Files.createDirectories(localRepo);
        Files.createFile(localRepo.resolve(".git"));

        PaperCodeEntry entry = PaperCodeEntry.builder()
                .id(2L)
                .repoUrl("https://github.com/foo/empty-repo.git")
                .branchName("main")
                .build();
        when(paperCodeEntryRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(entry));

        repoEntrySyncService.enqueue(1, "fatal: Needed a single revision");

        List<RepoEntrySyncResult> results = repoEntrySyncService.syncEntries(null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAction()).isEqualTo("SKIP");
        assertThat(results.get(0).getMessage()).contains("空");
    }

    @Test
    void duplicateDirectorySkipsSecondEntry() {
        PaperCodeEntry first = PaperCodeEntry.builder()
                .id(10L)
                .repoUrl("https://github.com/foo/shared-repo.git")
                .build();
        PaperCodeEntry second = PaperCodeEntry.builder()
                .id(11L)
                .repoUrl("https://gitee.com/bar/shared-repo.git")
                .build();
        when(paperCodeEntryRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(first, second));

        repoEntrySyncService.enqueue(0, "clone success");

        List<RepoEntrySyncResult> results = repoEntrySyncService.syncEntries(null, null);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getAction()).isEqualTo("CLONE");
        assertThat(results.get(1).getAction()).isEqualTo("SKIP");
        assertThat(results.get(1).getMessage()).contains("重复条目");
    }

    @Test
    void failurePathMessageIsSanitized() {
        PaperCodeEntry entry = PaperCodeEntry.builder()
                .id(20L)
                .repoUrl("https://github.com/foo/fail-repo.git")
                .build();
        when(paperCodeEntryRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(entry));

        repoEntrySyncService.enqueue(1,
                "fatal: could not read Password for 'https://user:pwd123@github.com/foo/fail-repo.git' "
                        + "token=abc123 /etc/personal-web/keys/id_ed25519");

        List<RepoEntrySyncResult> results = repoEntrySyncService.syncEntries(null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAction()).isEqualTo("FAILED");
        assertThat(results.get(0).getMessage()).doesNotContain("pwd123");
        assertThat(results.get(0).getMessage()).doesNotContain("abc123");
        assertThat(results.get(0).getMessage()).doesNotContain("/etc/personal-web/keys/id_ed25519");
        assertThat(results.get(0).getMessage()).contains("***");
    }

    private static final class StubRepoEntrySyncService extends RepoEntrySyncService {

        private final Deque<GitCommandResult> queue = new ArrayDeque<>();
        private final List<String> commands = new ArrayList<>();

        private StubRepoEntrySyncService(PaperCodeEntryRepository paperCodeEntryRepository) {
            super(paperCodeEntryRepository);
        }

        void enqueue(int exitCode, String output) {
            queue.addLast(new GitCommandResult(exitCode, output));
        }

        List<String> commands() {
            return commands;
        }

        @Override
        protected GitCommandResult runGitCommand(List<String> command, boolean useSsh) {
            commands.add(String.join(" ", command));
            if (queue.isEmpty()) {
                return new GitCommandResult(0, "");
            }
            return queue.removeFirst();
        }
    }
}
