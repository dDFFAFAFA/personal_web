package com.changye.web.service;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.model.PaperCodeEntry;
import com.changye.web.model.RepoConfig;
import com.changye.web.repository.PaperCodeEntryRepository;
import com.changye.web.repository.RepoConfigRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@Transactional
public class RepoReadmeService {

    private static final Long CONFIG_ID = 1L;

    private final RepoConfigRepository repoConfigRepository;
    private final PaperCodeEntryRepository paperCodeEntryRepository;

    @Value("${repo.base-workdir:/srv/personal-web/repos}")
    private String repoBaseWorkdir;

    @Value("${repo.ssh.private-key-path:/etc/personal-web/keys/id_ed25519}")
    private String sshPrivateKeyPath;

    @Value("${repo.command-timeout-seconds:120}")
    private long commandTimeoutSeconds;

    public RepoReadmeService(RepoConfigRepository repoConfigRepository,
                             PaperCodeEntryRepository paperCodeEntryRepository) {
        this.repoConfigRepository = repoConfigRepository;
        this.paperCodeEntryRepository = paperCodeEntryRepository;
    }

    public void rebuildReadme(boolean failWhenRepoMissing) {
        RepoConfig config = repoConfigRepository.findById(CONFIG_ID)
                .orElseThrow(() -> new BusinessException(400, "仓库配置不存在，请先保存仓库配置"));

        if (!StringUtils.hasText(config.getTargetDir())) {
            throw new BusinessException(400, "仓库目标目录未配置");
        }

        Path repoDir = resolveTargetPath(config.getTargetDir());
        if (!Files.exists(repoDir.resolve(".git"))) {
            if (failWhenRepoMissing) {
                throw new BusinessException(400, "总仓库尚未同步到本地，请先执行一次仓库同步");
            }
            log.warn("Skip README rebuild because repo not ready path={}", repoDir);
            return;
        }

        List<PaperCodeEntry> entries = paperCodeEntryRepository.findAllByOrderByUpdatedAtDesc();
        String readmeContent = buildReadme(entries, config.getAppBaseUrl());
        Path readmePath = repoDir.resolve("README.md");
        try {
            Files.writeString(readmePath, readmeContent, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BusinessException(500, "写入 README 失败");
        }

        if (Boolean.TRUE.equals(config.getAutoCommitReadme())) {
            commitAndPushReadme(repoDir, config.getBranchName());
        }
    }

    private String buildReadme(List<PaperCodeEntry> entries, String appBaseUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Paper Code Index\n\n");
        sb.append("自动维护的论文代码索引。\n\n");
        sb.append("| 论文 | 代码仓库 | 平台 | 分支 | 备注 | 更新时间 |\n");
        sb.append("|---|---|---|---|---|---|\n");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (PaperCodeEntry entry : entries) {
            String paperTitle = entry.getPaper().getTitle();
            String paperLink = StringUtils.hasText(appBaseUrl)
                    ? appBaseUrl.replaceAll("/$", "") + "/papers/" + entry.getPaper().getId()
                    : "/papers/" + entry.getPaper().getId();
            String paperCell = "[" + escapePipe(paperTitle) + "](" + paperLink + ")";
            String repoCell = "[repo](" + entry.getRepoUrl() + ")";
            String provider = entry.getProvider() == null ? "-" : entry.getProvider().name();
            String branch = StringUtils.hasText(entry.getBranchName()) ? entry.getBranchName() : "main";
            String desc = StringUtils.hasText(entry.getDescription()) ? escapePipe(entry.getDescription()) : "-";
            String updatedAt = entry.getUpdatedAt() == null ? "-" : entry.getUpdatedAt().format(fmt);
            sb.append("| ").append(paperCell)
                    .append(" | ").append(repoCell)
                    .append(" | ").append(provider)
                    .append(" | ").append(branch)
                    .append(" | ").append(desc)
                    .append(" | ").append(updatedAt)
                    .append(" |\n");
        }

        if (entries.isEmpty()) {
            sb.append("\n当前还没有论文代码条目。\n");
        }

        return sb.toString();
    }

    private void commitAndPushReadme(Path repoDir, String branchName) {
        if (!StringUtils.hasText(branchName)) {
            branchName = "main";
        }

        runGitCommand(List.of("git", "-C", repoDir.toString(), "add", "README.md"), true);
        CommandResult commitResult = runGitCommand(
                List.of("git", "-C", repoDir.toString(), "commit", "-m", "docs: update paper code index"),
                false
        );
        if (commitResult.exitCode != 0 && !commitResult.output.contains("nothing to commit")) {
            throw new BusinessException(500, "README 提交失败: " + trimMessage(commitResult.output));
        }
        if (commitResult.output.contains("nothing to commit")) {
            return;
        }

        CommandResult pushResult = runGitCommand(
                List.of("git", "-C", repoDir.toString(), "push", "origin", branchName),
                false
        );
        if (pushResult.exitCode != 0) {
            throw new BusinessException(500, "README 推送失败: " + trimMessage(pushResult.output));
        }
    }

    private CommandResult runGitCommand(List<String> command, boolean failOnError) {
        ProcessBuilder processBuilder = new ProcessBuilder(new ArrayList<>(command));
        processBuilder.redirectErrorStream(true);
        Path keyPath = Paths.get(sshPrivateKeyPath);
        if (Files.exists(keyPath)) {
            processBuilder.environment().put(
                    "GIT_SSH_COMMAND",
                    "ssh -i " + sshPrivateKeyPath + " -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new"
            );
        }

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(commandTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                if (failOnError) {
                    throw new BusinessException(500, "Git 命令执行超时");
                }
                return new CommandResult(-1, "timeout");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (failOnError && process.exitValue() != 0) {
                throw new BusinessException(500, "Git 命令失败: " + trimMessage(output));
            }
            return new CommandResult(process.exitValue(), output);
        } catch (IOException ex) {
            if (failOnError) {
                throw new BusinessException(500, "执行 Git 命令失败");
            }
            return new CommandResult(-1, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            if (failOnError) {
                throw new BusinessException(500, "执行 Git 命令被中断");
            }
            return new CommandResult(-1, "interrupted");
        }
    }

    private Path resolveTargetPath(String configuredTargetDir) {
        Path configured = Paths.get(configuredTargetDir);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        return Paths.get(repoBaseWorkdir).resolve(configured).normalize();
    }

    private String escapePipe(String text) {
        return text.replace("|", "\\|");
    }

    private String trimMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        String value = message.trim();
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    private record CommandResult(int exitCode, String output) {
    }
}
