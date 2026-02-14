package com.changye.web.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.response.PaperBackupStatusResponse;
import com.changye.web.model.Paper;
import com.changye.web.model.enums.BackupStatus;
import com.changye.web.repository.PaperRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
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
        ensureOssEnabled();
        Paper paper = findPaper(paperId);
        if (!StringUtils.hasText(paper.getFilePath())) {
            throw new BusinessException(400, "论文本地文件不存在，无法备份");
        }

        Path localFile = Paths.get(paper.getFilePath());
        if (!Files.exists(localFile)) {
            throw new BusinessException(404, "本地文件不存在，无法备份");
        }

        String objectKey = buildObjectKey(paper, localFile.getFileName().toString());
        OSS client = createClient();
        try {
            client.putObject(ossBucket, objectKey, localFile.toFile());
            paper.setOssObjectKey(objectKey);
            paper.setBackupStatus(BackupStatus.BACKED_UP);
            paper.setBackupAt(OffsetDateTime.now());
            paper.setBackupError(null);
            paperRepository.save(paper);
            log.info("Backup to OSS success paperId={} objectKey={}", paperId, objectKey);
            return toResponse(paper);
        } catch (Exception ex) {
            log.error("Backup to OSS failed paperId={}", paperId, ex);
            paper.setBackupStatus(BackupStatus.FAILED);
            paper.setBackupAt(OffsetDateTime.now());
            paper.setBackupError(trimError(ex.getMessage()));
            paperRepository.save(paper);
            throw new BusinessException(500, "备份到 OSS 失败");
        } finally {
            client.shutdown();
        }
    }

    @Transactional(readOnly = true)
    public PaperBackupStatusResponse getBackupStatus(Long paperId) {
        return toResponse(findPaper(paperId));
    }

    public PaperBackupStatusResponse restorePaper(Long paperId) {
        ensureOssEnabled();
        Paper paper = findPaper(paperId);
        if (!StringUtils.hasText(paper.getOssObjectKey())) {
            throw new BusinessException(400, "该论文尚未备份到 OSS");
        }

        Path restorePath = resolveRestorePath(paper);
        if (Files.exists(restorePath)) {
            throw new BusinessException(400, "本地文件已存在，无需恢复");
        }

        try {
            Files.createDirectories(restorePath.getParent());
        } catch (IOException ex) {
            throw new BusinessException(500, "恢复目录创建失败");
        }

        OSS client = createClient();
        try {
            if (!client.doesObjectExist(ossBucket, paper.getOssObjectKey())) {
                throw new BusinessException(404, "OSS 备份文件不存在");
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
            log.info("Restore from OSS success paperId={} path={}", paperId, restorePath);
            return toResponse(paper);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Restore from OSS failed paperId={}", paperId, ex);
            throw new BusinessException(500, "从 OSS 恢复失败");
        } finally {
            client.shutdown();
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

    private OSS createClient() {
        return new OSSClientBuilder().build(ossEndpoint, ossAccessKeyId, ossAccessKeySecret);
    }

    private String buildObjectKey(Paper paper, String localName) {
        String filename = StringUtils.hasText(paper.getFileName()) ? paper.getFileName() : localName;
        return "papers/" + paper.getId() + "/" + filename;
    }

    private Path resolveRestorePath(Paper paper) {
        if (StringUtils.hasText(paper.getFilePath())) {
            return Paths.get(paper.getFilePath());
        }
        String filename = StringUtils.hasText(paper.getFileName()) ? paper.getFileName() : (paper.getId() + "_paper.pdf");
        return Paths.get(localBasePath).toAbsolutePath().normalize().resolve(paper.getId() + "_" + filename);
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
}
