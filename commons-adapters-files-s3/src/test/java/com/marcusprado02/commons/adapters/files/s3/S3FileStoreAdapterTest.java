package com.marcusprado02.commons.adapters.files.s3;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.files.FileId;
import com.marcusprado02.commons.ports.files.FileObject;
import com.marcusprado02.commons.ports.files.FileStorePort;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class S3FileStoreAdapterTest {

  private static final String BUCKET_NAME = "test-bucket";
  private static final String TEST_CONTENT = "Hello, S3!";

  @Container
  @SuppressWarnings("resource")
  static LocalStackContainer localstack = new LocalStackContainer(
      DockerImageName.parse("localstack/localstack:3.0")
  ).withServices(S3);

  private static S3FileStoreAdapter adapter;
  private static S3Client s3Client;

  @BeforeAll
  static void setUp() {
    S3Configuration config = S3Configuration.builder()
        .region(localstack.getRegion())
        .endpoint(localstack.getEndpointOverride(S3))
        .pathStyleAccessEnabled(true)
        .build();

    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
    );

    s3Client = S3ClientFactory.createClient(config, credentialsProvider);
    adapter = S3ClientFactory.createAdapter(config, credentialsProvider);

    // Create test bucket
    s3Client.createBucket(b -> b.bucket(BUCKET_NAME));
  }

  @AfterAll
  static void tearDown() {
    if (s3Client != null) {
      s3Client.close();
    }
  }

  @Test
  @Order(1)
  @DisplayName("Should upload file successfully")
  void shouldUploadFile() {
    // Given
    FileId fileId = new FileId(BUCKET_NAME, "test-file.txt");
    InputStream content = new ByteArrayInputStream(TEST_CONTENT.getBytes(StandardCharsets.UTF_8));

    // When
    Result<FileStorePort.UploadResult> result = adapter.upload(fileId, content);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();
    assertThat(result.getOrNull().fileId()).isEqualTo(fileId);
    assertThat(result.getOrNull().etag()).isNotNull();
    assertThat(result.getOrNull().contentLength()).isEqualTo(TEST_CONTENT.length());
  }

  @Test
  @Order(2)
  @DisplayName("Should download file successfully")
  void shouldDownloadFile() throws IOException {
    // Given
    FileId fileId = new FileId(BUCKET_NAME, "test-file.txt");

    // When
    Result<FileObject> result = adapter.download(fileId);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();

    FileObject fileObject = result.getOrNull();
    assertThat(fileObject.id()).isEqualTo(fileId);
    assertThat(fileObject.metadata().contentType()).isNotNull();
    assertThat(fileObject.metadata().contentLength()).isGreaterThan(0);

    String content = new String(fileObject.content().readAllBytes(), StandardCharsets.UTF_8);
    assertThat(content).isEqualTo(TEST_CONTENT);
  }

  @Test
  @Order(3)
  @DisplayName("Should check if file exists")
  void shouldCheckFileExists() {
    // Given
    FileId existingFile = new FileId(BUCKET_NAME, "test-file.txt");
    FileId nonExistingFile = new FileId(BUCKET_NAME, "non-existing-file.txt");

    // When
    Result<Boolean> existsResult = adapter.exists(existingFile);
    Result<Boolean> notExistsResult = adapter.exists(nonExistingFile);

    // Then
    assertThat(existsResult.isOk()).isTrue();
    assertThat(existsResult.getOrNull()).isTrue();

    assertThat(notExistsResult.isOk()).isTrue();
    assertThat(notExistsResult.getOrNull()).isFalse();
  }

  @Test
  @Order(4)
  @DisplayName("Should get file metadata")
  void shouldGetMetadata() {
    // Given
    FileId fileId = new FileId(BUCKET_NAME, "test-file.txt");

    // When
    Result<FileObject.FileMetadata> result = adapter.getMetadata(fileId);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();

    FileObject.FileMetadata metadata = result.getOrNull();
    assertThat(metadata.contentType()).isNotNull();
    assertThat(metadata.contentLength()).isGreaterThan(0);
    assertThat(metadata.etag()).isNotNull();
  }

  @Test
  @Order(5)
  @DisplayName("Should list files with prefix")
  void shouldListFiles() {
    // Given
    FileId file1 = new FileId(BUCKET_NAME, "docs/file1.txt");
    FileId file2 = new FileId(BUCKET_NAME, "docs/file2.txt");
    FileId file3 = new FileId(BUCKET_NAME, "images/image1.png");

    adapter.upload(file1, new ByteArrayInputStream("content1".getBytes()));
    adapter.upload(file2, new ByteArrayInputStream("content2".getBytes()));
    adapter.upload(file3, new ByteArrayInputStream("content3".getBytes()));

    // When
    Result<FileStorePort.ListResult> result = adapter.list(BUCKET_NAME, "docs/");

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();

    FileStorePort.ListResult listResult = result.getOrNull();
    assertThat(listResult.files()).hasSize(2);
    assertThat(listResult.files()).containsExactlyInAnyOrder(file1, file2);
  }

  @Test
  @Order(6)
  @DisplayName("Should generate presigned GET URL")
  void shouldGeneratePresignedGetUrl() {
    // Given
    FileId fileId = new FileId(BUCKET_NAME, "test-file.txt");
    Duration duration = Duration.ofMinutes(15);

    // When
    Result<URL> result = adapter.generatePresignedUrl(
        fileId,
        FileStorePort.PresignedOperation.GET,
        duration
    );

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();

    URL url = result.getOrNull();
    assertThat(url.toString()).contains(BUCKET_NAME);
    assertThat(url.toString()).contains("test-file.txt");
    assertThat(url.getQuery()).contains("X-Amz-Signature");
  }

  @Test
  @Order(7)
  @DisplayName("Should generate presigned PUT URL")
  void shouldGeneratePresignedPutUrl() {
    // Given
    FileId fileId = new FileId(BUCKET_NAME, "upload-file.txt");
    Duration duration = Duration.ofMinutes(15);

    // When
    Result<URL> result = adapter.generatePresignedUrl(
        fileId,
        FileStorePort.PresignedOperation.PUT,
        duration
    );

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();

    URL url = result.getOrNull();
    assertThat(url.toString()).contains(BUCKET_NAME);
    assertThat(url.toString()).contains("upload-file.txt");
  }

  @Test
  @Order(8)
  @DisplayName("Should copy file")
  void shouldCopyFile() throws IOException {
    // Given
    FileId source = new FileId(BUCKET_NAME, "source-file.txt");
    FileId destination = new FileId(BUCKET_NAME, "destination-file.txt");

    adapter.upload(source, new ByteArrayInputStream(TEST_CONTENT.getBytes()));

    // When
    Result<Void> copyResult = adapter.copy(source, destination);

    // Then
    assertThat(copyResult.isOk()).isTrue();

    // Verify destination exists
    Result<FileObject> downloadResult = adapter.download(destination);
    assertThat(downloadResult.isOk()).isTrue();

    String content = new String(downloadResult.getOrNull().content().readAllBytes(), StandardCharsets.UTF_8);
    assertThat(content).isEqualTo(TEST_CONTENT);
  }

  @Test
  @Order(9)
  @DisplayName("Should delete single file")
  void shouldDeleteFile() {
    // Given
    FileId fileId = new FileId(BUCKET_NAME, "file-to-delete.txt");
    adapter.upload(fileId, new ByteArrayInputStream(TEST_CONTENT.getBytes()));

    // When
    Result<Void> result = adapter.delete(fileId);

    // Then
    assertThat(result.isOk()).isTrue();

    // Verify file is deleted
    Result<Boolean> existsResult = adapter.exists(fileId);
    assertThat(existsResult.isOk()).isTrue();
    assertThat(existsResult.getOrNull()).isFalse();
  }

  @Test
  @Order(10)
  @DisplayName("Should delete multiple files")
  void shouldDeleteMultipleFiles() {
    // Given
    FileId file1 = new FileId(BUCKET_NAME, "delete1.txt");
    FileId file2 = new FileId(BUCKET_NAME, "delete2.txt");
    FileId file3 = new FileId(BUCKET_NAME, "delete3.txt");

    adapter.upload(file1, new ByteArrayInputStream("content1".getBytes()));
    adapter.upload(file2, new ByteArrayInputStream("content2".getBytes()));
    adapter.upload(file3, new ByteArrayInputStream("content3".getBytes()));

    // When
    Result<FileStorePort.DeleteResult> result = adapter.deleteAll(List.of(file1, file2, file3));

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().deletedCount()).isEqualTo(3);
    assertThat(result.getOrNull().failedDeletes()).isEmpty();
  }

  @Test
  @Order(11)
  @DisplayName("Should upload file with custom options")
  void shouldUploadWithOptions() {
    // Given
    FileId fileId = new FileId(BUCKET_NAME, "file-with-options.txt");
    InputStream content = new ByteArrayInputStream(TEST_CONTENT.getBytes());

    FileStorePort.UploadOptions options = FileStorePort.UploadOptions.builder()
        .contentType("text/plain")
        .metadata(java.util.Map.of("author", "test-user", "version", "1.0"))
        .storageClass(FileStorePort.StorageClass.STANDARD)
        .build();

    // When
    Result<FileStorePort.UploadResult> result = adapter.upload(fileId, content, options);

    // Then
    assertThat(result.isOk()).isTrue();

    // Verify metadata
    Result<FileObject.FileMetadata> metadataResult = adapter.getMetadata(fileId);
    assertThat(metadataResult.isOk()).isTrue();
    assertThat(metadataResult.getOrNull().contentType()).isEqualTo("text/plain");
    assertThat(metadataResult.getOrNull().customMetadata())
        .containsEntry("author", "test-user")
        .containsEntry("version", "1.0");
  }

  @Test
  @Order(12)
  @DisplayName("Should handle download of non-existing file")
  void shouldHandleNonExistingFileDownload() {
    // Given
    FileId nonExistingFile = new FileId(BUCKET_NAME, "does-not-exist.txt");

    // When
    Result<FileObject> result = adapter.download(nonExistingFile);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull()).isNotNull();
    assertThat(result.problemOrNull().message()).isEqualTo("File Not Found");
  }

  @Test
  @Order(13)
  @DisplayName("Should handle metadata of non-existing file")
  void shouldHandleNonExistingFileMetadata() {
    // Given
    FileId nonExistingFile = new FileId(BUCKET_NAME, "does-not-exist.txt");

    // When
    Result<FileObject.FileMetadata> result = adapter.getMetadata(nonExistingFile);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull()).isNotNull();
    assertThat(result.problemOrNull().message()).isEqualTo("File Not Found");
  }

  @Test
  @DisplayName("Should handle FileId generation")
  void shouldHandleFileIdGeneration() {
    // When
    FileId generatedId = FileId.generate(BUCKET_NAME, "documents/");

    // Then
    assertThat(generatedId.bucket()).isEqualTo(BUCKET_NAME);
    assertThat(generatedId.key()).startsWith("documents/");
    assertThat(generatedId.toPath()).startsWith(BUCKET_NAME + "/documents/");
  }

  @Test
  @DisplayName("Should handle FileId from path")
  void shouldHandleFileIdFromPath() {
    // When
    FileId fileId = FileId.fromPath("my-bucket/folder/file.txt");

    // Then
    assertThat(fileId.bucket()).isEqualTo("my-bucket");
    assertThat(fileId.key()).isEqualTo("folder/file.txt");
    assertThat(fileId.getFileName()).isEqualTo("file.txt");
    assertThat(fileId.getExtension()).isEqualTo("txt");
  }
}
