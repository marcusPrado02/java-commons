package com.marcusprado02.commons.adapters.files.azureblob;

import com.azure.storage.blob.BlobServiceClient;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.files.FileId;
import com.marcusprado02.commons.ports.files.FileObject;
import com.marcusprado02.commons.ports.files.FileStorePort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AzureBlobFileStoreAdapter using Azurite (Azure Storage Emulator).
 */
@Testcontainers
class AzureBlobFileStoreAdapterTest {

  private static final String CONTAINER_NAME = "test-container";
  private static final int AZURITE_BLOB_PORT = 10000;

  @Container
  @SuppressWarnings("resource") // Container is managed by Testcontainers
  private static final GenericContainer<?> azuriteContainer = new GenericContainer<>(
      DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:latest"))
      .withCommand("azurite-blob", "--blobHost", "0.0.0.0", "--blobPort", String.valueOf(AZURITE_BLOB_PORT))
      .withExposedPorts(AZURITE_BLOB_PORT);

  private AzureBlobFileStoreAdapter adapter;
  private BlobServiceClient blobServiceClient;

  @BeforeEach
  void setUp() {
    String connectionString = String.format(
        "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://%s:%d/devstoreaccount1;",
        azuriteContainer.getHost(),
        azuriteContainer.getMappedPort(AZURITE_BLOB_PORT)
    );

    AzureBlobConfiguration config = AzureBlobConfiguration.azurite(connectionString);
    blobServiceClient = AzureBlobClientFactory.createClient(config);
    adapter = AzureBlobClientFactory.createAdapter(config);

    // Create test container
    blobServiceClient.createBlobContainerIfNotExists(CONTAINER_NAME);
  }

  @AfterEach
  void tearDown() {
    // Clean up test container
    if (blobServiceClient != null) {
      try {
        var containerClient = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
        if (containerClient.exists()) {
          containerClient.delete();
        }
      } catch (Exception e) {
        // Ignore cleanup errors
      }
    }
  }

  @Test
  void shouldUploadFile() {
    FileId fileId = new FileId(CONTAINER_NAME, "test-file.txt");
    String content = "Hello, Azure Blob!";
    InputStream inputStream = new ByteArrayInputStream(content.getBytes());

    FileStorePort.UploadOptions options = FileStorePort.UploadOptions.builder()
        .contentType("text/plain")
        .build();

    Result<FileStorePort.UploadResult> result = adapter.upload(fileId, inputStream, options);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().fileId()).isEqualTo(fileId);
    assertThat(result.getOrNull().contentLength()).isEqualTo((long) content.length());
    assertThat(result.getOrNull().etag()).isNotNull();
  }

  @Test
  void shouldUploadFileWithMetadata() {
    FileId fileId = new FileId(CONTAINER_NAME, "test-file-with-metadata.txt");
    String content = "Content with metadata";
    InputStream inputStream = new ByteArrayInputStream(content.getBytes());

    Map<String, String> metadata = Map.of(
        "author", "test-user",
        "category", "testing"
    );

    FileStorePort.UploadOptions options = FileStorePort.UploadOptions.builder()
        .contentType("text/plain")
        .metadata(metadata)
        .build();

    Result<FileStorePort.UploadResult> result = adapter.upload(fileId, inputStream, options);

    assertThat(result.isOk()).isTrue();

    // Verify metadata
    Result<FileObject.FileMetadata> metadataResult = adapter.getMetadata(fileId);
    assertThat(metadataResult.isOk()).isTrue();
    assertThat(metadataResult.getOrNull().customMetadata()).containsAllEntriesOf(metadata);
  }

  @Test
  void shouldDownloadFile() {
    FileId fileId = new FileId(CONTAINER_NAME, "download-test.txt");
    String content = "Download test content";

    // Upload first
    adapter.upload(fileId, new ByteArrayInputStream(content.getBytes()), FileStorePort.UploadOptions.builder().build());

    // Download
    Result<FileObject> result = adapter.download(fileId);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().id()).isEqualTo(fileId);
    assertThat(result.getOrNull().metadata().contentLength()).isEqualTo(content.length());
  }

  @Test
  void shouldCheckIfFileExists() {
    FileId existing = new FileId(CONTAINER_NAME, "existing-file.txt");
    FileId nonExisting = new FileId(CONTAINER_NAME, "non-existing-file.txt");

    // Upload file
    adapter.upload(existing, new ByteArrayInputStream("content".getBytes()), FileStorePort.UploadOptions.builder().build());

    Result<Boolean> existsResult = adapter.exists(existing);
    assertThat(existsResult.isOk()).isTrue();
    assertThat(existsResult.getOrNull()).isTrue();

    Result<Boolean> notExistsResult = adapter.exists(nonExisting);
    assertThat(notExistsResult.isOk()).isTrue();
    assertThat(notExistsResult.getOrNull()).isFalse();
  }

  @Test
  void shouldGetFileMetadata() {
    FileId fileId = new FileId(CONTAINER_NAME, "metadata-test.txt");
    String content = "Metadata test";
    String contentType = "text/plain";

    FileStorePort.UploadOptions options = FileStorePort.UploadOptions.builder()
        .contentType(contentType)
        .build();

    adapter.upload(fileId, new ByteArrayInputStream(content.getBytes()), options);

    Result<FileObject.FileMetadata> result = adapter.getMetadata(fileId);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().contentType()).isEqualTo(contentType);
    assertThat(result.getOrNull().contentLength()).isEqualTo(content.length());
    assertThat(result.getOrNull().etag()).isNotNull();
    assertThat(result.getOrNull().lastModified()).isNotNull();
  }

  @Test
  void shouldListFilesWithPrefix() {
    // Upload multiple files
    adapter.upload(new FileId(CONTAINER_NAME, "prefix/file1.txt"),
        new ByteArrayInputStream("content1".getBytes()), FileStorePort.UploadOptions.builder().build());
    adapter.upload(new FileId(CONTAINER_NAME, "prefix/file2.txt"),
        new ByteArrayInputStream("content2".getBytes()), FileStorePort.UploadOptions.builder().build());
    adapter.upload(new FileId(CONTAINER_NAME, "other/file3.txt"),
        new ByteArrayInputStream("content3".getBytes()), FileStorePort.UploadOptions.builder().build());

    FileStorePort.ListOptions options = new FileStorePort.ListOptions(100, null);
    Result<FileStorePort.ListResult> result = adapter.list(CONTAINER_NAME, "prefix/", options);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().files()).hasSize(2);
    assertThat(result.getOrNull().files()).extracting(FileId::key)
        .containsExactlyInAnyOrder("prefix/file1.txt", "prefix/file2.txt");
  }

  @Test
  void shouldGeneratePresignedUrlForGet() {
    FileId fileId = new FileId(CONTAINER_NAME, "presigned-get.txt");
    adapter.upload(fileId, new ByteArrayInputStream("content".getBytes()),
        FileStorePort.UploadOptions.builder().build());

    Result<URL> result = adapter.generatePresignedUrl(fileId, FileStorePort.PresignedOperation.GET, Duration.ofHours(1));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().toString()).contains("sig="); // SAS signature
    assertThat(result.getOrNull().toString()).contains(fileId.key());
  }

  @Test
  void shouldGeneratePresignedUrlForPut() {
    FileId fileId = new FileId(CONTAINER_NAME, "presigned-put.txt");

    Result<URL> result = adapter.generatePresignedUrl(fileId, FileStorePort.PresignedOperation.PUT, Duration.ofHours(1));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().toString()).contains("sig="); // SAS signature
  }

  @Test
  void shouldCopyFile() {
    FileId source = new FileId(CONTAINER_NAME, "source-file.txt");
    FileId destination = new FileId(CONTAINER_NAME, "destination-file.txt");

    String content = "Content to copy";
    adapter.upload(source, new ByteArrayInputStream(content.getBytes()),
        FileStorePort.UploadOptions.builder().build());

    Result<Void> copyResult = adapter.copy(source, destination);

    assertThat(copyResult.isOk()).isTrue();

    // Verify destination exists
    Result<Boolean> existsResult = adapter.exists(destination);
    assertThat(existsResult.isOk()).isTrue();
    assertThat(existsResult.getOrNull()).isTrue();
  }

  @Test
  void shouldDeleteFile() {
    FileId fileId = new FileId(CONTAINER_NAME, "delete-test.txt");
    adapter.upload(fileId, new ByteArrayInputStream("content".getBytes()),
        FileStorePort.UploadOptions.builder().build());

    Result<Void> deleteResult = adapter.delete(fileId);
    assertThat(deleteResult.isOk()).isTrue();

    Result<Boolean> existsResult = adapter.exists(fileId);
    assertThat(existsResult.getOrNull()).isFalse();
  }

  @Test
  void shouldDeleteMultipleFiles() {
    FileId file1 = new FileId(CONTAINER_NAME, "batch/file1.txt");
    FileId file2 = new FileId(CONTAINER_NAME, "batch/file2.txt");
    FileId file3 = new FileId(CONTAINER_NAME, "batch/file3.txt");

    adapter.upload(file1, new ByteArrayInputStream("content1".getBytes()), FileStorePort.UploadOptions.builder().build());
    adapter.upload(file2, new ByteArrayInputStream("content2".getBytes()), FileStorePort.UploadOptions.builder().build());
    adapter.upload(file3, new ByteArrayInputStream("content3".getBytes()), FileStorePort.UploadOptions.builder().build());

    Result<FileStorePort.DeleteResult> result = adapter.deleteAll(List.of(file1, file2, file3));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().deletedCount()).isEqualTo(3);
    assertThat(result.getOrNull().failedDeletes()).isEmpty();
  }

  @Test
  void shouldHandleDownloadNonExistentFile() {
    FileId nonExistent = new FileId(CONTAINER_NAME, "does-not-exist.txt");

    Result<FileObject> result = adapter.download(nonExistent);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).contains("FILE_NOT_FOUND");
  }

  @Test
  void shouldHandleGetMetadataNonExistentFile() {
    FileId nonExistent = new FileId(CONTAINER_NAME, "does-not-exist.txt");

    Result<FileObject.FileMetadata> result = adapter.getMetadata(nonExistent);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).contains("FILE_NOT_FOUND");
  }

  @Test
  void shouldGenerateUniqueFileId() {
    FileId fileId1 = FileId.generate(CONTAINER_NAME, "test");
    FileId fileId2 = FileId.generate(CONTAINER_NAME, "test");

    assertThat(fileId1).isNotEqualTo(fileId2);
    assertThat(fileId1.key()).contains("test");
    assertThat(fileId2.key()).contains("test");
  }

  @Test
  void shouldParseFileIdFromPath() {
    String path = CONTAINER_NAME + "/folder/subfolder/file.txt";
    FileId fileId = FileId.fromPath(path);

    assertThat(fileId.bucket()).isEqualTo(CONTAINER_NAME);
    assertThat(fileId.key()).isEqualTo("folder/subfolder/file.txt");
  }

  @Test
  void shouldExtractFileNameFromFileId() {
    FileId fileId = new FileId(CONTAINER_NAME, "folder/document.pdf");

    assertThat(fileId.getFileName()).isEqualTo("document.pdf");
    assertThat(fileId.getExtension()).isEqualTo("pdf");
  }

  @Test
  void shouldUploadWithStorageClass() {
    FileId fileId = new FileId(CONTAINER_NAME, "archive-file.txt");
    String content = "Archive content";

    FileStorePort.UploadOptions options = FileStorePort.UploadOptions.builder()
        .storageClass(FileStorePort.StorageClass.GLACIER)
        .build();

    Result<FileStorePort.UploadResult> result = adapter.upload(fileId,
        new ByteArrayInputStream(content.getBytes()), options);

    assertThat(result.isOk()).isTrue();
  }
}
