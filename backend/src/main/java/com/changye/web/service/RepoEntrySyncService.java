package com.changye.web.service;

import com.changye.web.model.PaperCodeEntry;
import com.changye.web.repository.PaperCodeEntryRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@Transactional
public class RepoEntrySyncService {

    private static final String ACTION_CLONE = "CLONE";
    private static final String ACTION_PULL = "PULL";
    private static final String ACTION_SKIP = "SKIP";
    private static final String ACTION_FAILED = "FAILED";

    private final PaperCodeEntryRepository paperCodeEntryRepository;

    @Value("${repo.base-workdir:/srv/personal-web/repos}")
    private String repoBaseWorkdir;

    @Value("${repo.ssh.private-key-path:/etc/personal-web/keys/id_ed25519}")
    private String sshPrivateKeyPath;

    @Value("${repo.command-timeout-seconds:120}")
    private long commandTimeoutSeconds;

    public RepoEntrySyncService(PaperCodeEntryRepository paperCodeEntryRepository) {
        this.paperCodeEntryRepository = paperCodeEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<RepoEntrySyncResult> syncEntries(Long paperId, List<Long> entryIds) {
        List<PaperCodeEntry> entries = resolveEntries(paperId, entryIds);
        if (entries.isEmpty()) {
            return List.of();
        }

        Path baseDir = Paths.get(repoBaseWorkdir).resolve("paper-code").toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException ex) {
            String message = "创建本地同步目录失败";
            return entries.stream().map(entry -> buildResult(entry, "", ACTION_FAILED, message)).toList();
        }

        Set<Path> touchedPaths = new HashSet<>();
        List<RepoEntrySyncResult> results = new ArrayList<>();
        for (PaperCodeEntry entry : entries) {
            results.add(syncSingleEntry(entry, baseDir, touchedPaths));
        }
        return results;
    }

    private List<PaperCodeEntry> resolveEntries(Long paperId, List<Long> entryIds) {
        List<PaperCodeEntry> base;
        if (entryIds != null && !entryIds.isEmpty()) {
            base = orderByInputIds(paperCodeEntryRepository.findAllById(entryIds), entryIds);
        } else if (paperId != null) {
            base = paperCodeEntryRepository.findByPaperIdOrderByUpdatedAtDesc(paperId);
        } else {
            base = paperCodeEntryRepository.findAllByOrderByUpdatedAtDesc();
        }

        if (paperId == null) {
            return base;
        }
        return base.stream()
                .filter(entry -> entry.getPaper() != null && paperId.equals(entry.getPaper().getId()))
                .toList();
    }

    private List<PaperCodeEntry> orderByInputIds(Collection<PaperCodeEntry> entries, List<Long> inputIds) {
        Deque<Long> order = new ArrayDeque<>(inputIds);
        List<PaperCodeEntry> sorted = new ArrayList<>();
        while (!order.isEmpty()) {
            Long id = order.pollFirst();
            entries.stream().filter(entry -> id.equals(entry.getId())).findFirst().ifPresent(sorted::add);
        }
        return sorted;
    }

    private RepoEntrySyncResult syncSingleEntry(PaperCodeEntry entry, Path baseDir, Set<Path> touchedPaths) {
        String repoUrl = entry.getRepoUrl();
        String repoName;
        try {
            repoName = extractRepoName(repoUrl);
        } catch (IllegalArgumentException ex) {
            return buildResult(entry, "", ACTION_FAILED, ex.getMessage());
        }

        Path localPath = baseDir.resolve(repoName).normalize();
        if (!localPath.startsWith(baseDir)) {
            return buildResult(entry, "", ACTION_FAILED, "本地路径非法");
        }
        String localPathText = localPath.toString();

        if (!touchedPaths.add(localPath)) {
            return buildResult(entry, localPathText, ACTION_SKIP, "目录已在本次任务中处理，跳过重复条目");
        }

        boolean useSsh = isSshRepoUrl(repoUrl);
        if (useSsh && !Files.exists(Paths.get(sshPrivateKeyPath))) {
            return buildResult(entry, localPathText, ACTION_FAILED, "SSH 私钥文件不存在，请先在服务器部署私钥");
        }

        try {
            if (Files.exists(localPath.resolve(".git"))) {
                return pullEntry(entry, localPath, useSsh);
            }
            if (Files.exists(localPath)) {
                return buildResult(entry, localPathText, ACTION_FAILED, "目标目录已存在且不是 Git 仓库");
            }
            return cloneEntry(entry, localPath, useSsh);
        } catch (IOException ex) {
            log.warn("repo_entry_sync_failed entryId={} repoUrl={} reason={}",
                    entry.getId(), sanitizeAndTrim(repoUrl), sanitizeAndTrim(ex.getMessage()));
            return buildResult(entry, localPathText, ACTION_FAILED, "本地目录准备失败");
        }
    }

    private RepoEntrySyncResult cloneEntry(PaperCodeEntry entry, Path localPath, boolean useSsh) throws IOException {
        Files.createDirectories(localPath.getParent());

        String targetBranch = normalizeBranch(entry.getBranchName());
        boolean fallback = false;
        List<String> cloneCommand = new ArrayList<>(List.of("git", "clone"));
        if (StringUtils.hasText(targetBranch)) {
            GitCommandResult check = runGitCommand(
                    List.of("git", "ls-remote", "--exit-code", "--heads", entry.getRepoUrl(), targetBranch),
                    useSsh
            );
            if (check.exitCode == 0) {
                cloneCommand.add("--branch");
                cloneCommand.add(targetBranch);
            } else {
                fallback = true;
            }
        }
        cloneCommand.add(entry.getRepoUrl());
        cloneCommand.add(localPath.toString());

        GitCommandResult cloneResult = runGitCommand(cloneCommand, useSsh);
        if (cloneResult.exitCode != 0) {
            if (isEmptyRepoMessage(cloneResult.output)) {
                return buildResult(entry, localPath.toString(), ACTION_SKIP, "远端仓库为空，已跳过");
            }
            return buildResult(entry, localPath.toString(), ACTION_FAILED,
                    "clone 失败: " + sanitizeAndTrim(cloneResult.output));
        }

        GitCommandResult headCheck = runGitCommand(
                List.of("git", "-C", localPath.toString(), "rev-parse", "--verify", "HEAD"),
                useSsh
        );
        if (headCheck.exitCode != 0) {
            return buildResult(entry, localPath.toString(), ACTION_SKIP, "远端仓库为空，已跳过");
        }

        if (fallback && StringUtils.hasText(targetBranch)) {
            String fallbackBranch = detectCurrentBranch(localPath, useSsh);
            if (!StringUtils.hasText(fallbackBranch)) {
                fallbackBranch = "默认分支";
            }
            return buildResult(entry, localPath.toString(), ACTION_CLONE,
                    "分支 " + targetBranch + " 不存在，已回退 " + fallbackBranch + " 并 clone");
        }
        return buildResult(entry, localPath.toString(), ACTION_CLONE, "clone 成功");
    }

    private RepoEntrySyncResult pullEntry(PaperCodeEntry entry, Path localPath, boolean useSsh) {
        GitCommandResult headCheck = runGitCommand(
                List.of("git", "-C", localPath.toString(), "rev-parse", "--verify", "HEAD"),
                useSsh
        );
        if (headCheck.exitCode != 0) {
            return buildResult(entry, localPath.toString(), ACTION_SKIP, "本地仓库为空，已跳过");
        }

        BranchResolution branchResolution = resolveBranchForPull(localPath, entry.getBranchName(), useSsh);

        List<String> checkoutCommand;
        if (branchResolution.checkoutFromRemote()) {
            checkoutCommand = List.of(
                    "git", "-C", localPath.toString(), "checkout", "-B",
                    branchResolution.branch(), "origin/" + branchResolution.branch()
            );
        } else {
            checkoutCommand = List.of("git", "-C", localPath.toString(), "checkout", branchResolution.branch());
        }
        GitCommandResult checkoutResult = runGitCommand(checkoutCommand, useSsh);
        if (checkoutResult.exitCode != 0) {
            return buildResult(entry, localPath.toString(), ACTION_FAILED,
                    "切换分支失败: " + sanitizeAndTrim(checkoutResult.output));
        }

        GitCommandResult pullResult = runGitCommand(
                List.of("git", "-C", localPath.toString(), "pull", "origin", branchResolution.branch()),
                useSsh
        );
        if (pullResult.exitCode != 0) {
            if (isEmptyRepoMessage(pullResult.output)) {
                return buildResult(entry, localPath.toString(), ACTION_SKIP, "远端仓库为空，已跳过");
            }
            return buildResult(entry, localPath.toString(), ACTION_FAILED,
                    "pull 失败: " + sanitizeAndTrim(pullResult.output));
        }

        if (branchResolution.fallback()) {
            return buildResult(entry, localPath.toString(), ACTION_PULL,
                    "分支 " + branchResolution.requestedBranch() + " 不存在，已回退默认分支 "
                            + branchResolution.branch() + " 并 pull");
        }
        return buildResult(entry, localPath.toString(), ACTION_PULL, "pull 成功");
    }

    private BranchResolution resolveBranchForPull(Path localPath, String requestedBranch, boolean useSsh) {
        String normalized = normalizeBranch(requestedBranch);
        if (!StringUtils.hasText(normalized)) {
            String current = detectCurrentBranch(localPath, useSsh);
            if (!StringUtils.hasText(current)) {
                current = detectDefaultBranch(localPath, useSsh);
            }
            if (!StringUtils.hasText(current)) {
                current = "main";
            }
            return new BranchResolution(current, false, false, "");
        }

        GitCommandResult localBranch = runGitCommand(
                List.of("git", "-C", localPath.toString(), "show-ref", "--verify", "--quiet", "refs/heads/" + normalized),
                useSsh
        );
        if (localBranch.exitCode == 0) {
            return new BranchResolution(normalized, false, false, normalized);
        }

        GitCommandResult remoteBranch = runGitCommand(
                List.of("git", "-C", localPath.toString(), "ls-remote", "--exit-code", "--heads", "origin", normalized),
                useSsh
        );
        if (remoteBranch.exitCode == 0) {
            return new BranchResolution(normalized, false, true, normalized);
        }

        String fallback = detectDefaultBranch(localPath, useSsh);
        if (!StringUtils.hasText(fallback)) {
            fallback = "main";
        }
        return new BranchResolution(fallback, true, false, normalized);
    }

    private String detectDefaultBranch(Path localPath, boolean useSsh) {
        GitCommandResult result = runGitCommand(
                List.of("git", "-C", localPath.toString(), "symbolic-ref", "--short", "refs/remotes/origin/HEAD"),
                useSsh
        );
        if (result.exitCode != 0 || !StringUtils.hasText(result.output)) {
            return "main";
        }
        String raw = result.output.trim();
        if (raw.contains("/")) {
            return raw.substring(raw.lastIndexOf('/') + 1);
        }
        return raw;
    }

    private String detectCurrentBranch(Path localPath, boolean useSsh) {
        GitCommandResult result = runGitCommand(
                List.of("git", "-C", localPath.toString(), "rev-parse", "--abbrev-ref", "HEAD"),
                useSsh
        );
        if (result.exitCode != 0 || !StringUtils.hasText(result.output)) {
            return "";
        }
        return result.output.trim();
    }

    String extractRepoName(String repoUrl) {
        if (!StringUtils.hasText(repoUrl)) {
            throw new IllegalArgumentException("仓库地址为空");
        }
        String value = repoUrl.trim();
        String pathPart;
        if (value.startsWith("git@")) {
            int colon = value.indexOf(':');
            if (colon <= 0 || colon + 1 >= value.length()) {
                throw new IllegalArgumentException("仓库地址格式非法");
            }
            pathPart = value.substring(colon + 1);
        } else {
            String normalized = value;
            if (normalized.startsWith("ssh://git@")) {
                normalized = normalized.replaceFirst("ssh://git@", "ssh://");
            }
            int scheme = normalized.indexOf("://");
            if (scheme < 0) {
                throw new IllegalArgumentException("仓库地址格式非法");
            }
            int firstSlash = normalized.indexOf('/', scheme + 3);
            if (firstSlash < 0 || firstSlash + 1 >= normalized.length()) {
                throw new IllegalArgumentException("仓库地址格式非法");
            }
            pathPart = normalized.substring(firstSlash + 1);
        }

        String cleaned = pathPart;
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        String[] parts = cleaned.split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("仓库地址格式非法");
        }
        String repoName = parts[parts.length - 1];
        if (repoName.endsWith(".git")) {
            repoName = repoName.substring(0, repoName.length() - 4);
        }
        if (!StringUtils.hasText(repoName)) {
            throw new IllegalArgumentException("无法解析仓库名");
        }
        if (!repoName.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("仓库名包含非法字符");
        }
        return repoName;
    }

    private boolean isSshRepoUrl(String repoUrl) {
        if (!StringUtils.hasText(repoUrl)) {
            return false;
        }
        String lower = repoUrl.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("git@") || lower.startsWith("ssh://");
    }

    protected GitCommandResult runGitCommand(List<String> command, boolean useSsh) {
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

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(commandTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new GitCommandResult(-1, "命令执行超时");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new GitCommandResult(process.exitValue(), output);
        } catch (IOException ex) {
            return new GitCommandResult(-1, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new GitCommandResult(-1, "命令执行被中断");
        }
    }

    private RepoEntrySyncResult buildResult(PaperCodeEntry entry, String localPath, String action, String message) {
        String safeMessage = sanitizeAndTrim(message);
        log.info("repo_entry_sync entryId={} action={} localPath={} msg={}",
                entry.getId(), action, sanitizeAndTrim(localPath), safeMessage);
        return RepoEntrySyncResult.builder()
                .entryId(entry.getId())
                .repoUrl(entry.getRepoUrl())
                .localPath(localPath)
                .action(action)
                .message(safeMessage)
                .build();
    }

    private boolean isEmptyRepoMessage(String output) {
        if (!StringUtils.hasText(output)) {
            return false;
        }
        String lower = output.toLowerCase(Locale.ROOT);
        return lower.contains("empty repository")
                || lower.contains("does not have any commits yet")
                || lower.contains("needed a single revision");
    }

    private String normalizeBranch(String branch) {
        return StringUtils.hasText(branch) ? branch.trim() : "";
    }

    private String sanitizeAndTrim(String message) {
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
        return sanitized.length() > 500 ? sanitized.substring(0, 500) : sanitized;
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

    protected static final class GitCommandResult {
        private final int exitCode;
        private final String output;

        protected GitCommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    private record BranchResolution(String branch, boolean fallback, boolean checkoutFromRemote, String requestedBranch) {
    }
}
