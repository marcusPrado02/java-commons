package com.marcusprado02.commons.adapters.files.gcs;

import static org.assertj.core.api.Assertions.*;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.marcusprado02.commons.ports.files.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class GCSFileStoreAdapterTest {

  private static final String PROJECT_ID = "test-project";
  private static final String BUCKET_NAME = "test-bucket";

  @Container
  private static final GenericContainer<?> fakeGcs =
      new GenericContainer<>("fsouza/fake-gcs-server:latest")
          .withExposedPorts(4443)
          .withCommand("-scheme", "http");

  private GCSFileStoreAdapter adapter;
  private Storage storage;

  private static byte[] convertStreamToBytes(java.io.InputStream stream) {
    try {
      return stream.readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeEach
  void setUp() {
    var endpoint = "http://" + fakeGcs.getHost() + ":" + fakeGcs.getMappedPort(4443);

    // Create storage client pointing to fake-gcs-server
    storage =
        StorageOptions.newBuilder().setProjectId(PROJECT_ID).setHost(endpoint).build().getService();

    // Create bucket
    storage.create(com.google.cloud.storage.BucketInfo.of(BUCKET_NAME));

    adapter = new GCSFileStoreAdapter(storage);
  }

  @Test
  void shouldUploadFile() {
    // Given
    var fileId = new FileId(BUCKET_NAME, "test.txt");
    var content = new ByteArrayInputStream("Hello GCS!".getBytes(StandardCharsets.UTF_8));
    var options = FileStorePort.UploadOptions.builder().contentType("text/plain").build();

    // When
    var result = adapter.upload(fileId, content, options);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().id()).isEqualTo(fileId);
    assertThat(result.getOrNull().contentLength()).isEqualTo(10L);
  }

  @Test
  void shouldUploadFileWithMetadata() {
    // Given
    var fileId = new FileId(BUCKET_NAME, "test-with-metadata.txt");
    var content = new ByteArrayInputStream("Test content".getBytes(StandardCharsets.UTF_8));
    var options =
        FileStorePort.UploadOptions.builder()
            .contentType("text/plain")
            .metadata(java.util.Map.of("author", "test-user", "version", "1.0"))
            .build();

    // When
    var result = adapter.upload(fileId, content, options);

    // Then
    assertThat(result.isOk()).isTrue();

    // Verify metadata
    var metadataResult = adapter.getMetadata(fileId);
    assertThat(metadataResult.isOk()).isTrue();
    assertThat(metadataResult.getOrNull().customMetadata())
        .containsEntry("author", "test-user")
        .containsEntry("version", "1.0");
  }

  @Test
  void shouldDownloadFile() {
    // Given
    var fileId = new FileId(BUCKET_NAME, "download-test.txt");
    var originalContent = "Download me!";
    adapter.upload(
        fileId,
        new ByteArrayInputStream(originalContent.getBytes(StandardCharsets.UTF_8)),
        FileStorePort.UploadOptions.defaults());

    // When
    var result = adapter.download(fileId);

    // Then
    assertThat(result.isOk()).isTrue();
    var fileObject = result.getOrNull();
    assertThat(fileObject.id()).isEqualTo(fileId);
    assertThat(new String(convertStreamToBytes(fileObject.content()), StandardCharsets.UTF_8))
        .isEqualTo(originalContent);
    assertThat(fileObject.customMetadata().contentLength()).isEqualTo(originalContent.length());
  }

  @Test
  void shouldReturnFailureWhenDownloadingNonExistentFile() {
    // Given
    var fileId = new FileId(BUCKET_NAME, "non-existent.txt");

    // When
    var result = adapter.download(fileId);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().title()).isEqualTo("File Not Found");
  }

  @Test
  void shouldCheckFileExists() {
    // Given
    var fileId = new FileId(BUCKET_NAME, "exists-test.txt");
    adapter.upload(
        fileId,
        new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)),
        FileStorePort.UploadOptions.defaults());

    // When
    var existsResult = adapter.exists(fileId);
    var notExistsResult = adapter.exists(new FileId(BUCKET_NAME, "does-not-exist.txt"));

    // Then
    assertThat(existsResult.isOk()).isTrue();
    assertThat(existsResult.getOrNull()).isTrue();

    assertThat(notExistsResult.isOk()).isTrue();
    assertThat(notExistsResult.getOrNull()).isFalse();
  }

  @Test
  void shouldGetMetadata() {
    // Given
    var fileId = new FileId(BUCKET_NAME, "metadata-test.txt");
    var content = "Test content for metadata";
    adapter.upload(
        fileId,
        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
        FileStorePort.UploadOptions.builder().contentType("text/plain").build());

    // When
    var result = adapter.getMetadata(fileId);

    // Then
    assertThat(result.isOk()).isTrue();
    var metadata = result.getOrNull();
    assertThat(metadata.contentLength()).isEqualTo(content.length());
    assertThat(metadata.contentType()).isEqualTo("text/plain");
    assertThat(metadata.etag()).isNotNull();
  }

  @Test
  void shouldListFilesWithPrefix() {
    // Given
    adapter.upload(
        new FileId(BUCKET_NAME, "documents/file1.txt"),
        new ByteArrayInputStream("content1".getBytes(StandardCharsets.UTF_8)),
        FileStorePort.UploadOptions.defaults());
    adapter.upload(
        new FileId(BUCKET_NAME, "documents/file2.txt"),
        new ByteArrayInputStream("content2".getBytes(StandardCharsets.UTF_8)),
        FileStorePort.UploadOptions.defaults());
    adapter.upload(
        new FileId(BUCKET_NAME, "images/photo.jpg"),
        new ByteArrayInputStream("photo".getBytes(StandardCharsets.UTF_8)),
        FileStorePort.UploadOptions.defaults());

    // When
    var result = adapter.list(BUCKET_NAME, "documents/", FileStorePort.ListOptions.defaults());

    // Then
    assertThat(result.isOk()).isTrue();
    var listResult = result.getOrNull();
    assertThat(listResult.files()).hasSize(2);
    assertThat(listResult.files())
        .extracting(FileId::key)
        .containsExactlyInAnyOrder("documents/file1.txt", "documents/file2.txt");
  }

  @Test
  void shouldGenerateSignedUrlForGet() {
    // Given
    var fileId = new FileId(BUCKET_NAME, "signed-url-test.txt");
    adapter.upload(
        fileId,
        new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)),
        FileStorePort.UploadOptions.defaults());

    // When
    var result =
        adapter.generatePresignedUrl(
            fileId, FileStorePort.PresignedOperation.GET, Duration.ofHours(1));

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();
    assertThat(result.getOrNull().toString()).contains(fileId.key());
  }

  @Test
  void shouldGenerateSignedUrlForPut() {
    // Given
    var fileId = new FileId(BUCKET_NAME, "upload-via-signed-url.txt");

    // When
    var result =
        adapter.generatePresignedUrl(
            fileId, FileStorePort.PresignedOperation.PUT, Duration.ofMinutes(15));

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();
  }

  @Test
  void shouldCopyFile() {
    // Given
    var source = new FileId(BUCKET_NAME, "source.txt");
    var destination = new FileId(BUCKET_NAME, "destination.txt");
    var content = "Content to copy";
    adapter.upload(
        source,
        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
        FileStorePort.UploadOptions.defaults());

    // When
    var copyResult = adapter.copy(source, destination);

    // Then
    assertThat(copyResult.isOk()).isTrue();

    // Verify destination exists with same content
    var downloadResult = adapter.download(destination);
    assertThat(downloadResult.isOk()).isTrue();
    assertThat(new String(downloadResult.getOrNull().content(), StandardCharsets.UTF_8))
        .isEqualTo(content);
  }

  @Test
  void shouldDeleteFile() {
    // Given
    var fileId = new FileId(BUCKET_NAME, "delete-test.txt");
    adapter.upload(
        fileId,
        new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)),
        FileStorePort.UploadOptions.defaults());

    // When
    var deleteResult = adapter.delete(fileId);

    // Then
    assertThat(deleteResult.isOk()).isTrue();

    // Verify file no longer exists
    var existsResult = adapter.exists(fileId);
    assertThat(existsResult.getOrNull()).isFalse();
  }

  @Test
  void shouldDeleteMultipleFiles() {
    // Given
    var file1 = new FileId(BUCKET_NAME, "batch/file1.txt");
    var file2 = new FileId(BUCKET_NAME, "batch/file2.txt");
    var file3 = new FileId(BUCKET_NAME, "batch/file3.txt");

    adapter.upload(
        file1,
        new ByteArrayInputStream("content1".getBytes(StandardCharsets.UTF_8)),
        FileStorePort.UploadOptions.defaults());
    adapter.upload(
        file2,
        new ByteArrayInputStream("content2".getBytes(StandardCharsets.UTF_8)),
        FileStorePort.UploadOptions.defaults());
    // file3 doesn't exist - should be in failed list

    // When
    var result = adapter.deleteAll(List.of(file1, file2, file3));

    // Then
    assertThat(result.isOk()).isTrue();
    var deleteResult = result.getOrNull();
    assertThat(deleteResult.deletedCount()).isEqualTo(2);
    assertThat(deleteResult.failedDeletes()).containsExactly(file3);
  }

  @Test
  void shouldHandleStorageClasses() {
    // Given
    var fileId = new FileId(BUCKET_NAME, "archive-file.txt");
    var options =
        FileStorePort.UploadOptions.builder()
            .contentType("text/plain")
            .storageClass(FileStorePort.StorageClass.DEEP_ARCHIVE)
            .build();

    // When
    var result =
        adapter.upload(
            fileId, new ByteArrayInputStream("archived".getBytes(StandardCharsets.UTF_8)), options);

    // Then
    assertThat(result.isOk()).isTrue();

    // Verify storage class (DEEP_ARCHIVE maps to ARCHIVE in GCS)
    var blob = storage.get(com.google.cloud.storage.BlobId.of(BUCKET_NAME, fileId.key()));
    assertThat(blob.getStorageClass()).isEqualTo(com.google.cloud.storage.StorageClass.ARCHIVE);
  }

  @Test
  void shouldReturnFailureWhenCopyingNonExistentFile() {
    // Given
    var source = new FileId(BUCKET_NAME, "non-existent-source.txt");
    var destination = new FileId(BUCKET_NAME, "destination.txt");

    // When
    var result = adapter.copy(source, destination);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().title()).isEqualTo("Source File Not Found");
  }

  @Test
  void shouldReturnFailureWhenDeletingNonExistentFile() {
    // Given
    var fileId = new FileId(BUCKET_NAME, "non-existent-delete.txt");

    // When
    var result = adapter.delete(fileId);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().title()).isEqualTo("File Not Found");
  }
}
