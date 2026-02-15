package com.changye.web.service;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.RepoConfigUpdateRequest;
import com.changye.web.dto.response.RepoConfigResponse;
import com.changye.web.dto.response.RepoSyncStatusResponse;
import com.changye.web.model.RepoConfig;
import com.changye.web.model.enums.RepoSyncMode;
import com.changye.web.model.enums.RepoSyncStatus;
import com.changye.web.repository.RepoConfigRepository;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@Transactional
public class RepoSyncService {

    private static final Long CONFIG_ID = 1L;

    private final RepoConfigRepository repoConfigRepository;

    @Value("${repo.base-workdir:/srv/personal-web/repos}")
    private String repoBaseWorkdir;

    @Value("${repo.ssh.private-key-path:/etc/personal-web/keys/id_ed25519}")
    private String sshPrivateKeyPath;

    @Value("${repo.command-timeout-seconds:120}")
    private long commandTimeoutSeconds;

    public RepoSyncService(RepoConfigRepository repoConfigRepository) {
        this.repoConfigRepository = repoConfigRepository;
    }

    @Transactional(readOnly = true)
    public RepoConfigResponse getConfig() {
        Optional<RepoConfig> optional = repoConfigRepository.findById(CONFIG_ID);
        return optional.map(this::toConfigResponse)
                .orElseGet(() -> RepoConfigResponse.builder()
                        .autoCommitReadme(true)
                        .sshKeyPath(maskPath(sshPrivateKeyPath))
                        .configured(false)
                        .build());
    }

    public RepoConfigResponse updateConfig(RepoConfigUpdateRequest request) {
        validateRepoUrl(request.getRepoUrl());
        RepoConfig config = repoConfigRepository.findById(CONFIG_ID).orElseGet(this::newConfig);
        config.setProvider(request.getProvider());
        config.setRepoUrl(request.getRepoUrl().trim());
        config.setBranchName(request.getBranch().trim());
        config.setTargetDir(request.getTargetDir().trim());
        if (request.getAutoCommitReadme() != null) {
            config.setAutoCommitReadme(request.getAutoCommitReadme());
        } else if (config.getAutoCommitReadme() == null) {
            config.setAutoCommitReadme(true);
        }
        config.setAppBaseUrl(StringUtils.hasText(request.getAppBaseUrl()) ? request.getAppBaseUrl().trim() : null);
        RepoConfig saved = repoConfigRepository.save(config);
        log.info("Repo config updated provider={} branch={} targetDir={}",
                saved.getProvider(), saved.getBranchName(), saved.getTargetDir());
        return toConfigResponse(saved);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public RepoSyncStatusResponse sync() {
        RepoConfig config = repoConfigRepository.findById(CONFIG_ID)
                .orElseThrow(() -> new BusinessException(404, "仓库配置不存在，请先保存配置"));
        long startedAt = System.nanoTime();
        RepoSyncMode mode = null;
        boolean useSsh = false;
        try {
            ensureConfigReady(config);

            useSsh = isSshRepoUrl(config.getRepoUrl());
            if (useSsh) {
                Path keyPath = Paths.get(sshPrivateKeyPath);
                if (!Files.exists(keyPath)) {
                    throw new BusinessException(400, "SSH 私钥文件不存在，请先在服务器部署私钥");
                }
            }

            Path targetPath = resolveTargetPath(config.getTargetDir());
            CommandResult result;
            if (Files.exists(targetPath.resolve(".git"))) {
                mode = RepoSyncMode.PULL;
                result = runGitCommand(List.of(
                        "git", "-C", targetPath.toString(), "pull", "origin", config.getBranchName()
                ), useSsh);
            } else {
                ensureCloneDirectoryAvailable(targetPath);
                mode = RepoSyncMode.CLONE;
                result = runGitCommand(List.of(
                        "git", "clone", "--branch", config.getBranchName(), config.getRepoUrl(), targetPath.toString()
                ), useSsh);
            }

            if (result.exitCode == 0) {
                OffsetDateTime now = OffsetDateTime.now();
                long durationMs = elapsedDurationMs(startedAt);
                String msg = "同步成功";
                updateSyncResult(config, RepoSyncStatus.SUCCESS, mode, msg, now, null, durationMs);
                log.info("Repo sync success mode={} target={} durationMs={}", mode, targetPath, durationMs);
                return RepoSyncStatusResponse.builder()
                        .status(RepoSyncStatus.SUCCESS)
                        .mode(mode)
                        .message(msg)
                        .syncedAt(now)
                        .durationMs(durationMs)
                        .build();
            }

            throw new BusinessException(500, "同步失败: " + sanitizeAndTrimMessage(result.output));
        } catch (IOException ex) {
            log.error("Repo sync process execution failed", ex);
            BusinessException wrapped = new BusinessException(500, "执行 git 命令失败");
            OffsetDateTime now = OffsetDateTime.now();
            long durationMs = elapsedDurationMs(startedAt);
            updateSyncResult(config, RepoSyncStatus.FAILED, mode, wrapped.getMessage(), now, wrapped.getCode(), durationMs);
            throw wrapped;
        } catch (BusinessException ex) {
            OffsetDateTime now = OffsetDateTime.now();
            long durationMs = elapsedDurationMs(startedAt);
            updateSyncResult(config, RepoSyncStatus.FAILED, mode, ex.getMessage(), now, ex.getCode(), durationMs);
            log.warn("Repo sync failed mode={} code={} durationMs={} message={}",
                    mode, ex.getCode(), durationMs, ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public RepoSyncStatusResponse getSyncStatus() {
        Optional<RepoConfig> optional = repoConfigRepository.findById(CONFIG_ID);
        if (optional.isEmpty()) {
            return RepoSyncStatusResponse.builder()
                    .status(RepoSyncStatus.IDLE)
                    .message("尚未配置仓库")
                    .build();
        }
        RepoConfig config = optional.get();
        return RepoSyncStatusResponse.builder()
                .status(config.getLastSyncStatus() == null ? RepoSyncStatus.IDLE : config.getLastSyncStatus())
                .mode(config.getLastSyncMode())
                .message(config.getLastSyncMessage())
                .syncedAt(config.getLastSyncAt())
                .errorCode(config.getLastSyncErrorCode())
                .durationMs(config.getLastSyncDurationMs())
                .build();
    }

    private void ensureConfigReady(RepoConfig config) {
        if (config.getProvider() == null
                || !StringUtils.hasText(config.getRepoUrl())
                || !StringUtils.hasText(config.getBranchName())
                || !StringUtils.hasText(config.getTargetDir())) {
            throw new BusinessException(400, "仓库配置不完整");
        }
    }

    private void validateRepoUrl(String repoUrl) {
        if (!StringUtils.hasText(repoUrl)) {
            throw new BusinessException(400, "仓库地址不能为空");
        }
        String value = repoUrl.trim();
        if (!(isAllowedHttpsUrl(value) || isAllowedSshUrl(value))) {
            throw new BusinessException(400, "仅支持 GitHub/Gitee 的 SSH 或 HTTPS 仓库地址");
        }
    }

    private boolean isAllowedHttpsUrl(String repoUrl) {
        String lower = repoUrl.toLowerCase();
        return lower.startsWith("https://github.com/") || lower.startsWith("https://gitee.com/");
    }

    private boolean isAllowedSshUrl(String repoUrl) {
        String lower = repoUrl.toLowerCase();
        if (lower.startsWith("git@github.com:") || lower.startsWith("git@gitee.com:")) {
            return true;
        }
        if (!lower.startsWith("ssh://")) {
            return false;
        }
        try {
            URI uri = new URI(repoUrl);
            String host = uri.getHost();
            return "github.com".equalsIgnoreCase(host) || "gitee.com".equalsIgnoreCase(host);
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private boolean isSshRepoUrl(String repoUrl) {
        if (!StringUtils.hasText(repoUrl)) {
            return false;
        }
        String lower = repoUrl.trim().toLowerCase();
        return lower.startsWith("git@") || lower.startsWith("ssh://");
    }

    private Path resolveTargetPath(String configuredTargetDir) {
        Path configured = Paths.get(configuredTargetDir);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        return Paths.get(repoBaseWorkdir).resolve(configured).normalize();
    }

    private void ensureCloneDirectoryAvailable(Path targetPath) throws IOException {
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(targetPath)) {
            return;
        }
        try (var stream = Files.list(targetPath)) {
            if (stream.findAny().isPresent()) {
                throw new BusinessException(400, "目标目录已存在且非空，无法执行 clone");
            }
        }
    }

    private CommandResult runGitCommand(List<String> command, boolean useSsh) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(new ArrayList<>(command));
        processBuilder.redirectErrorStream(true);
        if (useSsh) {
            processBuilder.environment().put(
                    "GIT_SSH_COMMAND",
                    "ssh -i " + sshPrivateKeyPath + " -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new"
            );
        } else {
            processBuilder.environment().remove("GIT_SSH_COMMAND");
        }

        Process process = processBuilder.start();
        String output;
        try {
            boolean finished = process.waitFor(commandTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(-1, "命令执行超时");
            }
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new CommandResult(process.exitValue(), output);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new CommandResult(-1, "命令执行被中断");
        }
    }

    private void updateSyncResult(RepoConfig config,
                                  RepoSyncStatus status,
                                  RepoSyncMode mode,
                                  String message,
                                  OffsetDateTime syncedAt,
                                  Integer errorCode,
                                  Long durationMs) {
        config.setLastSyncStatus(status);
        config.setLastSyncMode(mode);
        config.setLastSyncMessage(sanitizeAndTrimMessage(message));
        config.setLastSyncAt(syncedAt);
        config.setLastSyncErrorCode(errorCode);
        config.setLastSyncDurationMs(durationMs);
        repoConfigRepository.save(config);
    }

    private RepoConfigResponse toConfigResponse(RepoConfig config) {
        boolean configured = config.getProvider() != null
                && StringUtils.hasText(config.getRepoUrl())
                && StringUtils.hasText(config.getBranchName())
                && StringUtils.hasText(config.getTargetDir());
        return RepoConfigResponse.builder()
                .provider(config.getProvider())
                .repoUrl(config.getRepoUrl())
                .branch(config.getBranchName())
                .targetDir(config.getTargetDir())
                .autoCommitReadme(config.getAutoCommitReadme() == null ? true : config.getAutoCommitReadme())
                .appBaseUrl(config.getAppBaseUrl())
                .sshKeyPath(maskPath(sshPrivateKeyPath))
                .configured(configured)
                .build();
    }

    private RepoConfig newConfig() {
        return RepoConfig.builder()
                .id(CONFIG_ID)
                .lastSyncStatus(RepoSyncStatus.IDLE)
                .autoCommitReadme(true)
                .build();
    }

    private String maskPath(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        Path p = Paths.get(path);
        Path fileName = p.getFileName();
        if (fileName == null) {
            return "***";
        }
        return "***/" + fileName;
    }

    private String sanitizeAndTrimMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        String sanitized = message.trim()
                .replace(sshPrivateKeyPath, maskPath(sshPrivateKeyPath))
                .replaceAll("(?i)(authorization:\\s*bearer\\s+)[^\\s]+", "$1***")
                .replaceAll("(?i)(access_token=)[^\\s&]+", "$1***")
                .replaceAll("(?i)(token=)[^\\s&]+", "$1***")
                .replaceAll("(?i)(password=)[^\\s&]+", "$1***")
                .replaceAll("(?i)(https?://)([^\\s/@:]+):([^@\\s]+)@", "$1$2:***@");
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000);
        }
        return sanitized;
    }

    private long elapsedDurationMs(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - startedAtNanos));
    }

    private record CommandResult(int exitCode, String output) {
    }
}
