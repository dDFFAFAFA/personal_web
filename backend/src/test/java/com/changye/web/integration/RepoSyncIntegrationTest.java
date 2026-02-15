package com.changye.web.integration;

import com.changye.web.model.RepoConfig;
import com.changye.web.model.enums.RepoProvider;
import com.changye.web.model.enums.RepoSyncMode;
import com.changye.web.model.enums.RepoSyncStatus;
import com.changye.web.repository.RepoConfigRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "repo.command-timeout-seconds=1"
        })
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepoSyncIntegrationTest {

    private static final Path TEST_BASE_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "personal-web-repo-sync-it");
    private static final Path TEST_KEY_PATH = TEST_BASE_DIR.resolve("id_ed25519");

    @DynamicPropertySource
    static void overrideRepoProperties(DynamicPropertyRegistry registry) {
        registry.add("repo.base-workdir", () -> TEST_BASE_DIR.toString());
        registry.add("repo.ssh.private-key-path", () -> TEST_KEY_PATH.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepoConfigRepository repoConfigRepository;

    @BeforeAll
    void beforeAll() throws IOException {
        Files.createDirectories(TEST_BASE_DIR);
        Files.writeString(TEST_KEY_PATH, "dummy-key");
    }

    @BeforeEach
    void setUp() throws IOException {
        repoConfigRepository.deleteAll();
        cleanupTargetDirectories();
        Files.createDirectories(TEST_BASE_DIR);
        Files.writeString(TEST_KEY_PATH, "dummy-key");
    }

    @AfterAll
    void afterAll() throws IOException {
        cleanupTargetDirectories();
        Files.deleteIfExists(TEST_KEY_PATH);
    }

    @Test
    void syncReturns404WhenConfigMissing() throws Exception {
        mockMvc.perform(post("/api/v1/repo/sync"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("仓库配置不存在，请先保存配置"));
    }

    @Test
    void syncReturns400WhenPrivateKeyMissing() throws Exception {
        repoConfigRepository.save(buildConfig("missing-key"));
        Files.deleteIfExists(TEST_KEY_PATH);

        mockMvc.perform(post("/api/v1/repo/sync"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("SSH 私钥文件不存在，请先在服务器部署私钥"));

        RepoConfig persisted = repoConfigRepository.findById(1L).orElseThrow();
        assertThat(persisted.getLastSyncStatus()).isEqualTo(RepoSyncStatus.FAILED);
        assertThat(persisted.getLastSyncErrorCode()).isEqualTo(400);
        assertThat(persisted.getLastSyncDurationMs()).isNotNull();
    }

    @Test
    void syncReturns500AndPersistsFailureMetadata() throws Exception {
        repoConfigRepository.save(buildConfig("git-failed"));

        mockMvc.perform(post("/api/v1/repo/sync"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));

        RepoConfig persisted = repoConfigRepository.findById(1L).orElseThrow();
        assertThat(persisted.getLastSyncStatus()).isEqualTo(RepoSyncStatus.FAILED);
        assertThat(persisted.getLastSyncMode()).isEqualTo(RepoSyncMode.CLONE);
        assertThat(persisted.getLastSyncErrorCode()).isEqualTo(500);
        assertThat(persisted.getLastSyncDurationMs()).isNotNull();
        assertThat(persisted.getLastSyncDurationMs()).isGreaterThanOrEqualTo(0L);

        mockMvc.perform(get("/api/v1/repo/sync-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.errorCode").value(500))
                .andExpect(jsonPath("$.data.durationMs").isNumber());
    }

    private RepoConfig buildConfig(String targetSuffix) {
        return RepoConfig.builder()
                .id(1L)
                .provider(RepoProvider.GITHUB)
                .repoUrl("https://github.com/example/repo-does-not-exist.git")
                .branchName("main")
                .targetDir("sync-it-" + targetSuffix)
                .autoCommitReadme(true)
                .build();
    }

    private void cleanupTargetDirectories() throws IOException {
        if (!Files.exists(TEST_BASE_DIR)) {
            return;
        }
        try (var stream = Files.list(TEST_BASE_DIR)) {
            stream.filter(path -> path.getFileName().toString().startsWith("sync-it-"))
                    .forEach(this::deleteRecursivelyQuietly);
        }
    }

    private void deleteRecursivelyQuietly(Path root) {
        try {
            if (!Files.exists(root)) {
                return;
            }
            try (var walk = Files.walk(root)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                                // best effort cleanup for test temp directories
                            }
                        });
            }
        } catch (IOException ignored) {
            // best effort cleanup for test temp directories
        }
    }
}
