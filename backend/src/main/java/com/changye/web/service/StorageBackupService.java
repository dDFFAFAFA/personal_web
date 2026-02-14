package com.changye.web.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.response.PaperBackupStatusResponse;
import com.changye.web.model.Paper;
import com.changye.web.model.enums.BackupStatus;
import com.changye.web.repository.PaperRepository;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@Transactional
public class StorageBackupService {

    private final PaperRepository paperRepository;

    @Value("${storage.mode:local_only}")
    private String storageMode;

    @Value("${storage.local.base-path:${app.upload.path:./uploads/papers}}")
    private String localBasePath;

    @Value("${storage.oss.endpoint:}")
    private String ossEndpoint;

    @Value("${storage.oss.bucket:}")
    private String ossBucket;

    @Value("${storage.oss.access-key-id:}")
    private String ossAccessKeyId;

    @Value("${storage.oss.access-key-secret:}")
    private String ossAccessKeySecret;

    public StorageBackupService(PaperRepository paperRepository) {
        this.paperRepository = paperRepository;
    }

    public PaperBackupStatusResponse backupPaper(Long paperId) {
        long startedAtNanos = System.nanoTime();
        String status = "FAILED";
        String reason = "UNKNOWN";
        Paper paper = null;
        OSS client = null;
        try {
            ensureOssEnabled();
            paper = findPaper(paperId);
            Path localFile = resolveBackupFilePath(paper);
            String objectKey = buildObjectKey(paper, localFile.getFileName().toString());
            client = createClient();
            client.putObject(ossBucket, objectKey, localFile.toFile());
            paper.setOssObjectKey(objectKey);
            paper.setBackupStatus(BackupStatus.BACKED_UP);
            paper.setBackupAt(OffsetDateTime.now());
            paper.setBackupError(null);
            paperRepository.save(paper);
            status = "SUCCESS";
            reason = "OK";
            return toResponse(paper);
        } catch (Exception ex) {
            BusinessException mapped = mapStorageException(ex, "备份到 OSS 失败");
            reason = mapped.getMessage();
            if (paper != null) {
                paper.setBackupStatus(BackupStatus.FAILED);
                paper.setBackupAt(OffsetDateTime.now());
                paper.setBackupError(trimError(mapped.getMessage()));
                paperRepository.save(paper);
            }
            throw mapped;
        } finally {
            shutdownClient(client);
            logOperation("backup", paperId, status, startedAtNanos, reason);
        }
    }

    @Transactional(readOnly = true)
    public PaperBackupStatusResponse getBackupStatus(Long paperId) {
        return toResponse(findPaper(paperId));
    }

    public PaperBackupStatusResponse restorePaper(Long paperId) {
        long startedAtNanos = System.nanoTime();
        String status = "FAILED";
        String reason = "UNKNOWN";
        Paper paper = null;
        OSS client = null;
        try {
            ensureOssEnabled();
            paper = findPaper(paperId);
            if (!StringUtils.hasText(paper.getOssObjectKey())) {
                throw new BusinessException(400, "该论文尚未备份到 OSS");
            }
            Path restorePath = resolveRestorePath(paper);
            if (Files.exists(restorePath)) {
                throw new BusinessException(400, "本地文件已存在，无需恢复");
            }
            Files.createDirectories(restorePath.getParent());

            client = createClient();
            if (!client.doesObjectExist(ossBucket, paper.getOssObjectKey())) {
                throw new BusinessException(404, "OSS 对象不存在");
            }
            try (OSSObject object = client.getObject(ossBucket, paper.getOssObjectKey());
                 InputStream inputStream = object.getObjectContent()) {
                Files.copy(inputStream, restorePath);
            }
            paper.setFilePath(restorePath.toString());
            if (!StringUtils.hasText(paper.getFileName())) {
                paper.setFileName(restorePath.getFileName().toString());
            }
            paper.setFileSize(Files.size(restorePath));
            paper.setBackupStatus(BackupStatus.BACKED_UP);
            paper.setBackupError(null);
            paperRepository.save(paper);
            status = "SUCCESS";
            reason = "OK";
            return toResponse(paper);
        } catch (Exception ex) {
            BusinessException mapped = mapStorageException(ex, "从 OSS 恢复失败");
            reason = mapped.getMessage();
            if (paper != null) {
                if (mapped.getCode() >= 500 || mapped.getCode() == 403 || mapped.getCode() == 404) {
                    paper.setBackupStatus(BackupStatus.FAILED);
                }
                paper.setBackupError(trimError(mapped.getMessage()));
                paperRepository.save(paper);
            }
            throw mapped;
        } finally {
            shutdownClient(client);
            logOperation("restore", paperId, status, startedAtNanos, reason);
        }
    }

    private Paper findPaper(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));
    }

    private void ensureOssEnabled() {
        if (!"local_with_oss_backup".equalsIgnoreCase(storageMode)) {
            throw new BusinessException(400, "当前环境未启用 OSS 备份模式");
        }
        if (!StringUtils.hasText(ossEndpoint)
                || !StringUtils.hasText(ossBucket)
                || !StringUtils.hasText(ossAccessKeyId)
                || !StringUtils.hasText(ossAccessKeySecret)) {
            throw new BusinessException(400, "OSS 配置不完整");
        }
    }

    OSS createClient() {
        return new OSSClientBuilder().build(ossEndpoint, ossAccessKeyId, ossAccessKeySecret);
    }

    private String buildObjectKey(Paper paper, String localName) {
        String filename = resolveSafeFilename(
                StringUtils.hasText(paper.getFileName()) ? paper.getFileName() : localName,
                "paper.pdf");
        return "papers/" + paper.getId() + "/" + filename;
    }

    private Path resolveRestorePath(Paper paper) {
        Path basePath = resolveLocalBasePath();
        if (StringUtils.hasText(paper.getFilePath())) {
            Path configuredPath = toNormalizedPath(paper.getFilePath(), "本地恢复路径非法");
            ensurePathWithinBase(configuredPath, basePath, "本地恢复路径非法");
            return configuredPath;
        }
        String filename = resolveSafeFilename(paper.getFileName(), "paper.pdf");
        Path restorePath = basePath.resolve(paper.getId() + "_" + filename).normalize();
        ensurePathWithinBase(restorePath, basePath, "本地恢复路径非法");
        return restorePath;
    }

    private PaperBackupStatusResponse toResponse(Paper paper) {
        return PaperBackupStatusResponse.builder()
                .paperId(paper.getId())
                .backupStatus(paper.getBackupStatus())
                .backupAt(paper.getBackupAt())
                .backupError(paper.getBackupError())
                .ossObjectKey(paper.getOssObjectKey())
                .build();
    }

    private String trimError(String message) {
        if (!StringUtils.hasText(message)) {
            return "UNKNOWN";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private Path resolveBackupFilePath(Paper paper) {
        if (!StringUtils.hasText(paper.getFilePath())) {
            throw new BusinessException(400, "论文本地文件不存在，无法备份");
        }
        Path localFile = toNormalizedPath(paper.getFilePath(), "本地文件路径非法，无法备份");
        ensurePathWithinBase(localFile, resolveLocalBasePath(), "本地文件路径非法，无法备份");
        if (!Files.exists(localFile)) {
            throw new BusinessException(404, "本地文件不存在，无法备份");
        }
        if (!Files.isRegularFile(localFile)) {
            throw new BusinessException(400, "本地文件路径非法，无法备份");
        }
        return localFile;
    }

    private Path resolveLocalBasePath() {
        if (!StringUtils.hasText(localBasePath)) {
            throw new BusinessException(400, "本地存储路径未配置");
        }
        return toNormalizedPath(localBasePath, "本地存储路径配置非法");
    }

    private Path toNormalizedPath(String rawPath, String errorMessage) {
        try {
            return Paths.get(rawPath).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            throw new BusinessException(400, errorMessage);
        }
    }

    private void ensurePathWithinBase(Path targetPath, Path basePath, String errorMessage) {
        if (!targetPath.startsWith(basePath)) {
            throw new BusinessException(400, errorMessage);
        }
    }

    private String resolveSafeFilename(String rawName, String defaultName) {
        String resolved = StringUtils.hasText(rawName) ? rawName.trim() : defaultName;
        if (!StringUtils.hasText(resolved)
                || resolved.contains("/")
                || resolved.contains("\\")
                || resolved.contains("..")) {
            throw new BusinessException(400, "文件名非法");
        }
        return resolved;
    }

    private BusinessException mapStorageException(Exception ex, String defaultMessage) {
        if (ex instanceof BusinessException businessException) {
            return businessException;
        }
        if (ex instanceof OSSException ossException) {
            String errorCode = ossException.getErrorCode();
            if (isOss400(errorCode)) {
                return new BusinessException(400, "OSS 请求参数错误");
            }
            if (isOss403(errorCode)) {
                return new BusinessException(403, "OSS 访问被拒绝");
            }
            if (isOss404(errorCode)) {
                return new BusinessException(404, "OSS 对象不存在");
            }
            return new BusinessException(500, "OSS 服务异常");
        }
        if (ex instanceof ClientException) {
            return new BusinessException(500, "OSS 客户端连接失败");
        }
        return new BusinessException(500, defaultMessage);
    }

    private void shutdownClient(OSS client) {
        if (client != null) {
            try {
                client.shutdown();
            } catch (Exception ex) {
                log.warn("OSS client shutdown failed: {}", ex.getMessage());
            }
        }
    }

    private void logOperation(String operation, Long paperId, String status, long startedAtNanos, String reason) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
        if ("SUCCESS".equals(status)) {
            log.info("storage_backup operation={} paperId={} status={} durationMs={} reason={}",
                    operation, paperId, status, durationMs, reason);
            return;
        }
        log.warn("storage_backup operation={} paperId={} status={} durationMs={} reason={}",
                operation, paperId, status, durationMs, reason);
    }

    private boolean isOss400(String errorCode) {
        return "InvalidArgument".equalsIgnoreCase(errorCode)
                || "InvalidRequest".equalsIgnoreCase(errorCode)
                || "MalformedXML".equalsIgnoreCase(errorCode)
                || "BadRequest".equalsIgnoreCase(errorCode);
    }

    private boolean isOss403(String errorCode) {
        return "AccessDenied".equalsIgnoreCase(errorCode)
                || "InvalidAccessKeyId".equalsIgnoreCase(errorCode)
                || "SignatureDoesNotMatch".equalsIgnoreCase(errorCode)
                || "SecurityTokenExpired".equalsIgnoreCase(errorCode)
                || "RequestTimeTooSkewed".equalsIgnoreCase(errorCode);
    }

    private boolean isOss404(String errorCode) {
        return "NoSuchKey".equalsIgnoreCase(errorCode)
                || "NoSuchBucket".equalsIgnoreCase(errorCode)
                || "NotFound".equalsIgnoreCase(errorCode);
    }
}
