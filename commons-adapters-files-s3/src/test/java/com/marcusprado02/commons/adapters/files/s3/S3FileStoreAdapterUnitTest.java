package com.marcusprado02.commons.adapters.files.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.ports.files.FileId;
import com.marcusprado02.commons.ports.files.FileStorePort;
import com.marcusprado02.commons.ports.files.FileStorePort.PresignedOperation;
import com.marcusprado02.commons.ports.files.FileStorePort.ServerSideEncryption;
import com.marcusprado02.commons.ports.files.FileStorePort.StorageClass;
import com.marcusprado02.commons.ports.files.FileStorePort.UploadOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

/** Unit tests covering error paths and uncovered branches in S3FileStoreAdapter. */
@ExtendWith(MockitoExtension.class)
class S3FileStoreAdapterUnitTest {

  @Mock private S3Client s3Client;
  @Mock private S3Presigner s3Presigner;

  private S3FileStoreAdapter adapter;
  private final FileId fileId = new FileId("test-bucket", "test-key.txt");

  @BeforeEach
  void setUp() {
    S3Configuration config =
        S3Configuration.builder()
            .region("us-east-1")
            .endpoint((java.net.URI) null)
            .pathStyleAccessEnabled(false)
            .build();
    adapter = new S3FileStoreAdapter(s3Client, s3Presigner, config);
  }

  // ── upload error paths ────────────────────────────────────────────────────

  @Test
  void uploadShouldReturnFailOnS3Exception() {
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(S3Exception.builder().message("access denied").build());

    var result = adapter.upload(fileId, new ByteArrayInputStream("data".getBytes()));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to upload");
  }

  @Test
  void uploadShouldReturnFailOnIOException() throws IOException {
    InputStream brokenStream = mock(InputStream.class);
    when(brokenStream.readAllBytes()).thenThrow(new IOException("disk full"));

    var result = adapter.upload(fileId, brokenStream);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to read file content");
  }

  @Test
  void uploadShouldReturnFailOnUnexpectedException() {
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(new RuntimeException("unexpected"));

    var result = adapter.upload(fileId, new ByteArrayInputStream("data".getBytes()));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  @Test
  void uploadShouldApplyEncryptionWhenSpecified() {
    PutObjectResponse response = PutObjectResponse.builder().eTag("etag").build();
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(response);

    UploadOptions options =
        UploadOptions.builder()
            .contentType("text/plain")
            .encryption(ServerSideEncryption.AES256)
            .build();

    var result = adapter.upload(fileId, new ByteArrayInputStream("data".getBytes()), options);

    assertThat(result.isOk()).isTrue();
    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
    assertThat(captor.getValue().serverSideEncryption())
        .isEqualTo(software.amazon.awssdk.services.s3.model.ServerSideEncryption.AES256);
  }

  @Test
  void uploadShouldApplyAwsKmsEncryption() {
    PutObjectResponse response = PutObjectResponse.builder().eTag("etag").build();
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(response);

    UploadOptions options =
        UploadOptions.builder()
            .contentType("text/plain")
            .encryption(ServerSideEncryption.AWS_KMS)
            .build();

    var result = adapter.upload(fileId, new ByteArrayInputStream("data".getBytes()), options);

    assertThat(result.isOk()).isTrue();
    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
    assertThat(captor.getValue().serverSideEncryption())
        .isEqualTo(software.amazon.awssdk.services.s3.model.ServerSideEncryption.AWS_KMS);
  }

  @ParameterizedTest
  @EnumSource(
      value = StorageClass.class,
      names = {
        "REDUCED_REDUNDANCY",
        "STANDARD_IA",
        "ONEZONE_IA",
        "INTELLIGENT_TIERING",
        "GLACIER",
        "GLACIER_IR",
        "DEEP_ARCHIVE"
      })
  void uploadShouldMapAllStorageClasses(StorageClass storageClass) {
    PutObjectResponse response = PutObjectResponse.builder().eTag("etag").build();
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(response);

    UploadOptions options =
        UploadOptions.builder().contentType("text/plain").storageClass(storageClass).build();

    var result = adapter.upload(fileId, new ByteArrayInputStream("data".getBytes()), options);

    assertThat(result.isOk()).isTrue();
  }

  // ── download error paths ──────────────────────────────────────────────────

  @Test
  void downloadShouldReturnFailOnS3Exception() {
    when(s3Client.getObject(any(GetObjectRequest.class)))
        .thenThrow(S3Exception.builder().message("forbidden").build());

    var result = adapter.download(fileId);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to download");
  }

  @Test
  void downloadShouldReturnFailOnUnexpectedException() {
    when(s3Client.getObject(any(GetObjectRequest.class)))
        .thenThrow(new RuntimeException("unexpected"));

    var result = adapter.download(fileId);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── delete error paths ────────────────────────────────────────────────────

  @Test
  void deleteShouldReturnFailOnS3Exception() {
    doThrow(S3Exception.builder().message("forbidden").build())
        .when(s3Client)
        .deleteObject(any(DeleteObjectRequest.class));

    var result = adapter.delete(fileId);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to delete");
  }

  @Test
  void deleteShouldReturnFailOnUnexpectedException() {
    doThrow(new RuntimeException("unexpected"))
        .when(s3Client)
        .deleteObject(any(DeleteObjectRequest.class));

    var result = adapter.delete(fileId);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── deleteAll error paths ─────────────────────────────────────────────────

  @Test
  void deleteAllShouldReturnFailOnS3Exception() {
    doThrow(S3Exception.builder().message("forbidden").build())
        .when(s3Client)
        .deleteObjects(any(DeleteObjectsRequest.class));

    var result = adapter.deleteAll(List.of(fileId));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to delete files");
  }

  @Test
  void deleteAllShouldReturnFailOnUnexpectedException() {
    doThrow(new RuntimeException("unexpected"))
        .when(s3Client)
        .deleteObjects(any(DeleteObjectsRequest.class));

    var result = adapter.deleteAll(List.of(fileId));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── exists error paths ────────────────────────────────────────────────────

  @Test
  void existsShouldReturnFailOnS3Exception() {
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenThrow(S3Exception.builder().message("forbidden").build());

    var result = adapter.exists(fileId);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to check file existence");
  }

  @Test
  void existsShouldReturnFailOnUnexpectedException() {
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenThrow(new RuntimeException("unexpected"));

    var result = adapter.exists(fileId);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── getMetadata error paths ───────────────────────────────────────────────

  @Test
  void getMetadataShouldReturnFailOnS3Exception() {
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenThrow(S3Exception.builder().message("forbidden").build());

    var result = adapter.getMetadata(fileId);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to get file metadata");
  }

  @Test
  void getMetadataShouldReturnFailOnUnexpectedException() {
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenThrow(new RuntimeException("unexpected"));

    var result = adapter.getMetadata(fileId);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── list paths ────────────────────────────────────────────────────────────

  @Test
  void listShouldUseContinuationTokenWhenProvided() {
    ListObjectsV2Response response =
        ListObjectsV2Response.builder()
            .contents(S3Object.builder().key("file.txt").build())
            .nextContinuationToken(null)
            .isTruncated(false)
            .build();
    when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

    FileStorePort.ListOptions options = new FileStorePort.ListOptions(10, "next-page-token");

    var result = adapter.list("test-bucket", "prefix/", options);

    assertThat(result.isOk()).isTrue();
    ArgumentCaptor<ListObjectsV2Request> captor =
        ArgumentCaptor.forClass(ListObjectsV2Request.class);
    verify(s3Client).listObjectsV2(captor.capture());
    assertThat(captor.getValue().continuationToken()).isEqualTo("next-page-token");
  }

  @Test
  void listShouldReturnFailOnS3Exception() {
    when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenThrow(S3Exception.builder().message("forbidden").build());

    var result = adapter.list("test-bucket", "prefix/");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to list files");
  }

  @Test
  void listShouldReturnFailOnUnexpectedException() {
    when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenThrow(new RuntimeException("unexpected"));

    var result = adapter.list("test-bucket", "prefix/");

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── generatePresignedUrl error paths ──────────────────────────────────────

  @Test
  void generatePresignedUrlShouldReturnFailOnS3Exception() {
    when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
        .thenThrow(S3Exception.builder().message("forbidden").build());

    var result =
        adapter.generatePresignedUrl(fileId, PresignedOperation.GET, Duration.ofMinutes(15));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to generate presigned URL");
  }

  @Test
  void generatePresignedUrlShouldReturnFailOnUnexpectedException() {
    when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
        .thenThrow(new RuntimeException("unexpected"));

    var result =
        adapter.generatePresignedUrl(fileId, PresignedOperation.GET, Duration.ofMinutes(15));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── copy error paths ──────────────────────────────────────────────────────

  @Test
  void copyShouldReturnFailOnS3Exception() {
    FileId destination = new FileId("dest-bucket", "dest-key.txt");
    doThrow(S3Exception.builder().message("forbidden").build())
        .when(s3Client)
        .copyObject(any(CopyObjectRequest.class));

    var result = adapter.copy(fileId, destination);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to copy file");
  }

  @Test
  void copyShouldReturnFailOnUnexpectedException() {
    FileId destination = new FileId("dest-bucket", "dest-key.txt");
    doThrow(new RuntimeException("unexpected"))
        .when(s3Client)
        .copyObject(any(CopyObjectRequest.class));

    var result = adapter.copy(fileId, destination);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }
}
