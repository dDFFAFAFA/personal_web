package com.changye.web.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import com.changye.web.common.exception.BusinessException;
import com.changye.web.model.Paper;
import com.changye.web.model.enums.BackupStatus;
import com.changye.web.repository.PaperRepository;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageBackupServiceTest {

    @Mock
    private PaperRepository paperRepository;

    @Mock
    private OSS ossClient;

    private StorageBackupService storageBackupService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageBackupService = org.mockito.Mockito.spy(new StorageBackupService(paperRepository));
        ReflectionTestUtils.setField(storageBackupService, "storageMode", "local_with_oss_backup");
        ReflectionTestUtils.setField(storageBackupService, "localBasePath", tempDir.toString());
        ReflectionTestUtils.setField(storageBackupService, "ossEndpoint", "oss-cn-hangzhou.aliyuncs.com");
        ReflectionTestUtils.setField(storageBackupService, "ossBucket", "bucket");
        ReflectionTestUtils.setField(storageBackupService, "ossAccessKeyId", "ak");
        ReflectionTestUtils.setField(storageBackupService, "ossAccessKeySecret", "sk");
        lenient().doReturn(ossClient).when(storageBackupService).createClient();
    }

    @ParameterizedTest
    @MethodSource("ossBackupErrorMappings")
    void backupPaperMapsOssErrors(String errorCode, int expectedCode, String expectedMessage) throws Exception {
        Path file = Files.createFile(tempDir.resolve("paper.pdf"));
        Paper paper = buildPaper(1L, file.toString(), "paper.pdf", "papers/1/paper.pdf");
        mockPaper(paper);
        doThrow(ossException(errorCode)).when(ossClient).putObject(anyString(), anyString(), any(File.class));

        assertThatThrownBy(() -> storageBackupService.backupPaper(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(expectedCode);
                    assertThat(businessException.getMessage()).isEqualTo(expectedMessage);
                });

        assertThat(paper.getBackupStatus()).isEqualTo(BackupStatus.FAILED);
        assertThat(paper.getBackupError()).isEqualTo(expectedMessage);
        verify(paperRepository).save(paper);
    }

    @Test
    void backupPaperRejectsPathOutsideBase() throws Exception {
        Path outsideFile = Files.createTempFile("outside-paper", ".pdf");
        Paper paper = buildPaper(2L, outsideFile.toString(), "paper.pdf", null);
        mockPaper(paper);

        assertThatThrownBy(() -> storageBackupService.backupPaper(2L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(400);
                    assertThat(businessException.getMessage()).isEqualTo("本地文件路径非法，无法备份");
                });

        verify(storageBackupService, never()).createClient();
    }

    @Test
    void backupPaperReturns404WhenLocalFileMissing() {
        Paper paper = buildPaper(3L, tempDir.resolve("missing.pdf").toString(), "paper.pdf", null);
        mockPaper(paper);

        assertThatThrownBy(() -> storageBackupService.backupPaper(3L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(404);
                    assertThat(businessException.getMessage()).isEqualTo("本地文件不存在，无法备份");
                });
    }

    @Test
    void backupPaperReturns400WhenOssConfigMissing() {
        ReflectionTestUtils.setField(storageBackupService, "ossBucket", "");

        assertThatThrownBy(() -> storageBackupService.backupPaper(4L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(400);
                    assertThat(businessException.getMessage()).isEqualTo("OSS 配置不完整");
                });

        verify(paperRepository, never()).findById(anyLong());
    }

    @Test
    void restorePaperReturns404WhenObjectMissing() {
        Paper paper = buildPaper(5L, null, "paper.pdf", "papers/5/paper.pdf");
        mockPaper(paper);
        when(ossClient.doesObjectExist("bucket", "papers/5/paper.pdf")).thenReturn(false);

        assertThatThrownBy(() -> storageBackupService.restorePaper(5L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(404);
                    assertThat(businessException.getMessage()).isEqualTo("OSS 对象不存在");
                });

        assertThat(paper.getBackupStatus()).isEqualTo(BackupStatus.FAILED);
    }

    @Test
    void restorePaperRejectsUnsafeRestorePath() throws Exception {
        Path outsidePath = Files.createTempFile("restore-outside", ".pdf");
        Files.deleteIfExists(outsidePath);
        Paper paper = buildPaper(6L, outsidePath.toString(), "paper.pdf", "papers/6/paper.pdf");
        mockPaper(paper);

        assertThatThrownBy(() -> storageBackupService.restorePaper(6L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(400);
                    assertThat(businessException.getMessage()).isEqualTo("本地恢复路径非法");
                });

        verify(storageBackupService, never()).createClient();
    }

    @Test
    void restorePaperMapsAccessDeniedTo403() {
        Paper paper = buildPaper(7L, null, "paper.pdf", "papers/7/paper.pdf");
        mockPaper(paper);
        when(ossClient.doesObjectExist("bucket", "papers/7/paper.pdf")).thenReturn(true);
        when(ossClient.getObject("bucket", "papers/7/paper.pdf")).thenThrow(ossException("AccessDenied"));

        assertThatThrownBy(() -> storageBackupService.restorePaper(7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(403);
                    assertThat(businessException.getMessage()).isEqualTo("OSS 访问被拒绝");
                });

        assertThat(paper.getBackupStatus()).isEqualTo(BackupStatus.FAILED);
    }

    @Test
    void restorePaperSuccessWritesLocalFile() throws Exception {
        Paper paper = buildPaper(8L, null, "paper.pdf", "papers/8/paper.pdf");
        mockPaper(paper);
        when(ossClient.doesObjectExist("bucket", "papers/8/paper.pdf")).thenReturn(true);
        OSSObject ossObject = mock(OSSObject.class);
        when(ossClient.getObject("bucket", "papers/8/paper.pdf")).thenReturn(ossObject);
        when(ossObject.getObjectContent()).thenReturn(new ByteArrayInputStream("pdf-data".getBytes()));

        storageBackupService.restorePaper(8L);

        Path restoredFile = tempDir.resolve("8_paper.pdf");
        assertThat(Files.exists(restoredFile)).isTrue();
        assertThat(paper.getBackupStatus()).isEqualTo(BackupStatus.BACKED_UP);
        assertThat(paper.getBackupError()).isNull();
    }

    private void mockPaper(Paper paper) {
        when(paperRepository.findById(paper.getId())).thenReturn(Optional.of(paper));
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Paper buildPaper(Long id, String filePath, String fileName, String objectKey) {
        Paper paper = Paper.builder()
                .id(id)
                .title("paper-" + id)
                .filePath(filePath)
                .fileName(fileName)
                .ossObjectKey(objectKey)
                .backupStatus(BackupStatus.BACKED_UP)
                .build();
        paper.setBackupStatus(BackupStatus.BACKED_UP);
        return paper;
    }

    private OSSException ossException(String errorCode) {
        return new OSSException("oss error", errorCode, "requestId", "hostId", "header", "object", "GET");
    }

    private static Stream<Arguments> ossBackupErrorMappings() {
        return Stream.of(
                Arguments.of("InvalidArgument", 400, "OSS 请求参数错误"),
                Arguments.of("AccessDenied", 403, "OSS 访问被拒绝"),
                Arguments.of("NoSuchKey", 404, "OSS 对象不存在"),
                Arguments.of("UnknownError", 500, "OSS 服务异常")
        );
    }
}
