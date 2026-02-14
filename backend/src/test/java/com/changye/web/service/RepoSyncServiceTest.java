package com.changye.web.service;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.RepoConfigUpdateRequest;
import com.changye.web.dto.response.RepoConfigResponse;
import com.changye.web.model.RepoConfig;
import com.changye.web.model.enums.RepoProvider;
import com.changye.web.repository.RepoConfigRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepoSyncServiceTest {

    @Mock
    private RepoConfigRepository repoConfigRepository;

    private RepoSyncService repoSyncService;

    @BeforeEach
    void setUp() {
        repoSyncService = new RepoSyncService(repoConfigRepository);
        ReflectionTestUtils.setField(repoSyncService, "sshPrivateKeyPath", "/etc/personal-web/keys/id_ed25519");
    }

    @Test
    void updateConfigAcceptsGithubSshUrl() {
        RepoConfigUpdateRequest request = new RepoConfigUpdateRequest();
        request.setProvider(RepoProvider.GITHUB);
        request.setRepoUrl("git@github.com:foo/bar.git");
        request.setBranch("main");
        request.setTargetDir("repo");

        when(repoConfigRepository.findById(1L)).thenReturn(Optional.empty());
        when(repoConfigRepository.save(any(RepoConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RepoConfigResponse response = repoSyncService.updateConfig(request);

        assertThat(response.getProvider()).isEqualTo(RepoProvider.GITHUB);
        assertThat(response.getRepoUrl()).isEqualTo("git@github.com:foo/bar.git");
        assertThat(response.isConfigured()).isTrue();
    }

    @Test
    void updateConfigRejectsUnsupportedSshHost() {
        RepoConfigUpdateRequest request = new RepoConfigUpdateRequest();
        request.setProvider(RepoProvider.GITHUB);
        request.setRepoUrl("git@gitlab.com:foo/bar.git");
        request.setBranch("main");
        request.setTargetDir("repo");

        assertThatThrownBy(() -> repoSyncService.updateConfig(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(400);
                    assertThat(be.getMessage()).isEqualTo("仅支持 GitHub/Gitee 的 SSH 或 HTTPS 仓库地址");
                });
        verify(repoConfigRepository, never()).save(any());
    }

    @Test
    void sanitizeMessageMasksSecretsAndPrivateKeyPath() {
        String raw = "authorization: Bearer sk-secret-token\\n"
                + "fatal: could not read Password for 'https://user:pwd123@github.com/foo/bar.git'\\n"
                + "token=abc123&access_token=xyz999\\n"
                + "ssh -i /etc/personal-web/keys/id_ed25519";

        String sanitized = ReflectionTestUtils.invokeMethod(repoSyncService, "sanitizeAndTrimMessage", raw);

        assertThat(sanitized).doesNotContain("sk-secret-token");
        assertThat(sanitized).doesNotContain("pwd123");
        assertThat(sanitized).doesNotContain("abc123");
        assertThat(sanitized).doesNotContain("xyz999");
        assertThat(sanitized).doesNotContain("/etc/personal-web/keys/id_ed25519");
        assertThat(sanitized).contains("authorization: Bearer ***");
        assertThat(sanitized).contains("https://user:***@github.com/foo/bar.git");
        assertThat(sanitized).contains("***/id_ed25519");
    }
}
