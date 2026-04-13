package com.marcusprado02.commons.adapters.files.azureblob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.BlockBlobItem;
import com.marcusprado02.commons.ports.files.FileId;
import com.marcusprado02.commons.ports.files.FileStorePort.ListOptions;
import com.marcusprado02.commons.ports.files.FileStorePort.PresignedOperation;
import com.marcusprado02.commons.ports.files.FileStorePort.StorageClass;
import com.marcusprado02.commons.ports.files.FileStorePort.UploadOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for AzureBlobFileStoreAdapter covering exception and branch paths. */
@ExtendWith(MockitoExtension.class)
class AzureBlobFileStoreAdapterUnitTest {

  @Mock private BlobServiceClient blobServiceClient;
  @Mock private BlobContainerClient containerClient;
  @Mock private BlobClient blobClient;

  private AzureBlobFileStoreAdapter adapter;

  private static final AzureBlobConfiguration CONFIG =
      AzureBlobConfiguration.withConnectionString(
          "DefaultEndpointsProtocol=https;AccountName=test");
  private static final FileId FILE_ID = new FileId("container", "blob.txt");
  private static final UploadOptions DEFAULT_OPTIONS = UploadOptions.builder().build();

  @BeforeEach
  void setUp() {
    lenient()
        .when(blobServiceClient.getBlobContainerClient("container"))
        .thenReturn(containerClient);
    lenient().when(containerClient.getBlobClient("blob.txt")).thenReturn(blobClient);
    adapter = new AzureBlobFileStoreAdapter(blobServiceClient, CONFIG);
  }

  // ── upload ────────────────────────────────────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void uploadShouldSucceedWithStorageClassStandardIa() {
    Response<BlockBlobItem> response = mock(Response.class);
    BlockBlobItem item = mock(BlockBlobItem.class);
    when(item.getETag()).thenReturn("etag");
    when(response.getValue()).thenReturn(item);
    when(blobClient.uploadWithResponse(any(), any(), any())).thenReturn(response);

    UploadOptions options = UploadOptions.builder().storageClass(StorageClass.STANDARD_IA).build();
    var result = adapter.upload(FILE_ID, new ByteArrayInputStream("x".getBytes()), options);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  void uploadShouldSucceedWithStorageClassDeepArchive() {
    Response<BlockBlobItem> response = mock(Response.class);
    BlockBlobItem item = mock(BlockBlobItem.class);
    when(item.getETag()).thenReturn("etag");
    when(response.getValue()).thenReturn(item);
    when(blobClient.uploadWithResponse(any(), any(), any())).thenReturn(response);

    UploadOptions options = UploadOptions.builder().storageClass(StorageClass.DEEP_ARCHIVE).build();
    var result = adapter.upload(FILE_ID, new ByteArrayInputStream("x".getBytes()), options);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  void uploadShouldSucceedWithStorageClassStandard() {
    Response<BlockBlobItem> response = mock(Response.class);
    BlockBlobItem item = mock(BlockBlobItem.class);
    when(item.getETag()).thenReturn("etag");
    when(response.getValue()).thenReturn(item);
    when(blobClient.uploadWithResponse(any(), any(), any())).thenReturn(response);

    UploadOptions options = UploadOptions.builder().storageClass(StorageClass.STANDARD).build();
    var result = adapter.upload(FILE_ID, new ByteArrayInputStream("x".getBytes()), options);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void uploadShouldReturnFailOnBlobStorageException() {
    BlobStorageException ex = mock(BlobStorageException.class);
    when(ex.getMessage()).thenReturn("access denied");
    when(blobClient.uploadWithResponse(any(), any(), any())).thenThrow(ex);

    var result = adapter.upload(FILE_ID, new ByteArrayInputStream("x".getBytes()), DEFAULT_OPTIONS);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to upload blob to Azure");
  }

  @Test
  void uploadShouldReturnFailOnIOException() throws Exception {
    InputStream brokenStream = mock(InputStream.class);
    when(brokenStream.readAllBytes()).thenThrow(new IOException("disk error"));

    var result = adapter.upload(FILE_ID, brokenStream, DEFAULT_OPTIONS);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to read file content");
  }

  @Test
  void uploadShouldReturnFailOnRuntimeException() {
    when(blobClient.uploadWithResponse(any(), any(), any()))
        .thenThrow(new RuntimeException("network error"));

    var result = adapter.upload(FILE_ID, new ByteArrayInputStream("x".getBytes()), DEFAULT_OPTIONS);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── download ──────────────────────────────────────────────────────────────

  @Test
  void downloadShouldReturnFailOnBlobNotFound() {
    BlobStorageException ex = mock(BlobStorageException.class);
    when(ex.getErrorCode()).thenReturn(BlobErrorCode.BLOB_NOT_FOUND);
    when(blobClient.openInputStream()).thenThrow(ex);

    var result = adapter.download(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Blob does not exist");
  }

  @Test
  void downloadShouldReturnFailOnOtherBlobStorageException() {
    BlobStorageException ex = mock(BlobStorageException.class);
    when(ex.getErrorCode()).thenReturn(BlobErrorCode.AUTHORIZATION_FAILURE);
    when(ex.getMessage()).thenReturn("auth failed");
    when(blobClient.openInputStream()).thenThrow(ex);

    var result = adapter.download(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to download blob from Azure");
  }

  @Test
  void downloadShouldReturnFailOnRuntimeException() {
    when(blobClient.openInputStream()).thenThrow(new RuntimeException("unexpected"));

    var result = adapter.download(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  void deleteShouldReturnFailOnBlobStorageException() {
    BlobStorageException ex = mock(BlobStorageException.class);
    when(ex.getMessage()).thenReturn("not found");
    doThrow(ex).when(blobClient).delete();

    var result = adapter.delete(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to delete blob from Azure");
  }

  @Test
  void deleteShouldReturnFailOnRuntimeException() {
    doThrow(new RuntimeException("timeout")).when(blobClient).delete();

    var result = adapter.delete(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── deleteAll ─────────────────────────────────────────────────────────────

  @Test
  void deleteAllShouldHandleBlobStorageExceptionOnIndividualDelete() {
    BlobStorageException ex = mock(BlobStorageException.class);
    when(ex.getMessage()).thenReturn("blob error");
    doThrow(ex).when(blobClient).delete();

    var result = adapter.deleteAll(List.of(FILE_ID));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().deletedCount()).isEqualTo(0);
    assertThat(result.getOrNull().failedDeletes()).hasSize(1);
  }

  @Test
  void deleteAllShouldReturnFailOnOuterException() {
    when(blobServiceClient.getBlobContainerClient(any()))
        .thenThrow(new RuntimeException("connection lost"));

    var result = adapter.deleteAll(List.of(FILE_ID));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── exists ────────────────────────────────────────────────────────────────

  @Test
  void existsShouldReturnFailOnBlobStorageException() {
    BlobStorageException ex = mock(BlobStorageException.class);
    when(ex.getMessage()).thenReturn("access denied");
    when(blobClient.exists()).thenThrow(ex);

    var result = adapter.exists(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to check blob existence");
  }

  @Test
  void existsShouldReturnFailOnRuntimeException() {
    when(blobClient.exists()).thenThrow(new RuntimeException("network error"));

    var result = adapter.exists(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── getMetadata ───────────────────────────────────────────────────────────

  @Test
  void getMetadataShouldSucceed() {
    BlobProperties properties = mock(BlobProperties.class);
    when(properties.getContentType()).thenReturn("text/plain");
    when(properties.getBlobSize()).thenReturn(42L);
    when(properties.getLastModified()).thenReturn(OffsetDateTime.now());
    when(properties.getETag()).thenReturn("etag-abc");
    when(properties.getMetadata()).thenReturn(Map.of("key", "val"));
    when(blobClient.getProperties()).thenReturn(properties);

    var result = adapter.getMetadata(FILE_ID);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().contentType()).isEqualTo("text/plain");
  }

  @Test
  void getMetadataShouldReturnFailOnBlobNotFound() {
    BlobStorageException ex = mock(BlobStorageException.class);
    when(ex.getErrorCode()).thenReturn(BlobErrorCode.BLOB_NOT_FOUND);
    when(blobClient.getProperties()).thenThrow(ex);

    var result = adapter.getMetadata(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Blob does not exist");
  }

  @Test
  void getMetadataShouldReturnFailOnOtherBlobStorageException() {
    BlobStorageException ex = mock(BlobStorageException.class);
    when(ex.getErrorCode()).thenReturn(BlobErrorCode.AUTHORIZATION_FAILURE);
    when(ex.getMessage()).thenReturn("auth error");
    when(blobClient.getProperties()).thenThrow(ex);

    var result = adapter.getMetadata(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to get blob metadata");
  }

  @Test
  void getMetadataShouldReturnFailOnRuntimeException() {
    when(blobClient.getProperties()).thenThrow(new RuntimeException("timeout"));

    var result = adapter.getMetadata(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── list ──────────────────────────────────────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void listShouldSucceedWithResults() {
    BlobItem blobItem = mock(BlobItem.class);
    when(blobItem.getName()).thenReturn("blob.txt");

    PagedIterable<BlobItem> pagedIterable = mock(PagedIterable.class);
    when(pagedIterable.iterator()).thenReturn(List.of(blobItem).iterator());
    when(containerClient.listBlobs(any(), any())).thenReturn(pagedIterable);

    var result = adapter.list("container", "prefix/", new ListOptions(10, null));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().files()).hasSize(1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void listShouldStopAtMaxKeys() {
    BlobItem item1 = mock(BlobItem.class);
    when(item1.getName()).thenReturn("a.txt");
    BlobItem item2 = mock(BlobItem.class);
    when(item2.getName()).thenReturn("b.txt");
    BlobItem item3 = mock(BlobItem.class); // getName() not stubbed — never reached at maxKeys=2

    PagedIterable<BlobItem> pagedIterable = mock(PagedIterable.class);
    when(pagedIterable.iterator()).thenReturn(List.of(item1, item2, item3).iterator());
    when(containerClient.listBlobs(any(), any())).thenReturn(pagedIterable);

    var result = adapter.list("container", "", new ListOptions(2, null));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().files()).hasSize(2);
  }

  @Test
  void listShouldReturnFailOnBlobStorageException() {
    BlobStorageException ex = mock(BlobStorageException.class);
    when(ex.getMessage()).thenReturn("auth error");
    when(containerClient.listBlobs(any(), any())).thenThrow(ex);

    var result = adapter.list("container", "", new ListOptions(10, null));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to list blobs");
  }

  @Test
  void listShouldReturnFailOnRuntimeException() {
    when(containerClient.listBlobs(any(), any())).thenThrow(new RuntimeException("timeout"));

    var result = adapter.list("container", "", new ListOptions(10, null));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── generatePresignedUrl ──────────────────────────────────────────────────

  @Test
  void generatePresignedUrlShouldReturnFailOnBlobStorageException() {
    BlobStorageException ex = mock(BlobStorageException.class);
    when(ex.getMessage()).thenReturn("sas error");
    when(blobClient.generateSas(any())).thenThrow(ex);

    var result = adapter.generatePresignedUrl(FILE_ID, PresignedOperation.GET, Duration.ofHours(1));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to generate SAS URL");
  }

  @Test
  void generatePresignedUrlShouldReturnFailOnRuntimeException() {
    when(blobClient.generateSas(any())).thenThrow(new RuntimeException("timeout"));

    var result = adapter.generatePresignedUrl(FILE_ID, PresignedOperation.GET, Duration.ofHours(1));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  @Test
  void generatePresignedUrlShouldSupportPutOperation() {
    when(blobClient.generateSas(any())).thenReturn("sig=abc&se=...");
    when(blobClient.getBlobUrl())
        .thenReturn("https://account.blob.core.windows.net/container/blob.txt");

    var result =
        adapter.generatePresignedUrl(FILE_ID, PresignedOperation.PUT, Duration.ofMinutes(30));

    assertThat(result.isOk()).isTrue();
  }

  // ── copy ──────────────────────────────────────────────────────────────────

  @Test
  void copyShouldReturnFailOnBlobStorageException() {
    FileId dest = new FileId("container", "copy.txt");
    when(containerClient.getBlobClient("copy.txt")).thenReturn(blobClient);

    BlobStorageException ex = mock(BlobStorageException.class);
    when(ex.getMessage()).thenReturn("copy error");
    when(blobClient.getBlobUrl()).thenReturn("https://source-url");
    doThrow(ex).when(blobClient).copyFromUrl(any());

    var result = adapter.copy(FILE_ID, dest);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to copy blob");
  }

  @Test
  void copyShouldReturnFailOnRuntimeException() {
    FileId dest = new FileId("container", "copy.txt");
    when(containerClient.getBlobClient("copy.txt")).thenReturn(blobClient);
    when(blobClient.getBlobUrl()).thenReturn("https://source-url");
    doThrow(new RuntimeException("timeout")).when(blobClient).copyFromUrl(any());

    var result = adapter.copy(FILE_ID, dest);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Unexpected error");
  }

  // ── AzureBlobConfiguration builder ───────────────────────────────────────

  @Test
  void builderShouldBuildWithAllFields() {
    AzureBlobConfiguration config =
        AzureBlobConfiguration.builder()
            .endpoint("https://account.blob.core.windows.net")
            .accountName("myaccount")
            .connectionString("DefaultEndpointsProtocol=https;AccountName=myaccount")
            .maxRetries(5)
            .timeoutSeconds(60)
            .build();

    assertThat(config.endpoint()).isEqualTo("https://account.blob.core.windows.net");
    assertThat(config.accountName()).isEqualTo("myaccount");
    assertThat(config.maxRetries()).isEqualTo(5);
    assertThat(config.timeoutSeconds()).isEqualTo(60);
    assertThat(config.authenticationType())
        .isEqualTo(AzureBlobConfiguration.AuthenticationType.CONNECTION_STRING);
  }

  @Test
  void builderShouldSetSasTokenAuthType() {
    AzureBlobConfiguration config =
        AzureBlobConfiguration.builder()
            .endpoint("https://account.blob.core.windows.net")
            .sasToken("?sv=2023&sig=abc")
            .build();

    assertThat(config.authenticationType())
        .isEqualTo(AzureBlobConfiguration.AuthenticationType.SAS_TOKEN);
    assertThat(config.sasToken()).isEqualTo("?sv=2023&sig=abc");
  }

  @Test
  void builderShouldSetManagedIdentityAuthType() {
    AzureBlobConfiguration config =
        AzureBlobConfiguration.builder()
            .endpoint("https://account.blob.core.windows.net")
            .useManagedIdentity(true)
            .build();

    assertThat(config.authenticationType())
        .isEqualTo(AzureBlobConfiguration.AuthenticationType.MANAGED_IDENTITY);
    assertThat(config.useManagedIdentity()).isTrue();
  }

  @Test
  void builderShouldSetManagedIdentityFalseWithoutChangingAuthType() {
    AzureBlobConfiguration config =
        AzureBlobConfiguration.builder()
            .connectionString("DefaultEndpointsProtocol=https;AccountName=test")
            .useManagedIdentity(false)
            .build();

    assertThat(config.authenticationType())
        .isEqualTo(AzureBlobConfiguration.AuthenticationType.CONNECTION_STRING);
  }

  @Test
  void builderShouldSupportAuthenticationType() {
    AzureBlobConfiguration config =
        AzureBlobConfiguration.builder()
            .authenticationType(AzureBlobConfiguration.AuthenticationType.MANAGED_IDENTITY)
            .endpoint("https://account.blob.core.windows.net")
            .accountName("myaccount")
            .build();

    assertThat(config.authenticationType())
        .isEqualTo(AzureBlobConfiguration.AuthenticationType.MANAGED_IDENTITY);
  }

  // ── AzureBlobClientFactory.extractAccountName ──────────────────────────

  @Test
  void extractAccountNameShouldParseConnectionString() {
    String cs =
        "DefaultEndpointsProtocol=https;AccountName=myaccount;AccountKey=abc==;EndpointSuffix=core.windows.net";
    assertThat(AzureBlobClientFactory.extractAccountName(cs)).isEqualTo("myaccount");
  }
}
