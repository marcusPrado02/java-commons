package com.marcusprado02.commons.adapters.files.gcs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.marcusprado02.commons.ports.files.FileId;
import com.marcusprado02.commons.ports.files.FileStorePort.ListOptions;
import com.marcusprado02.commons.ports.files.FileStorePort.PresignedOperation;
import com.marcusprado02.commons.ports.files.FileStorePort.StorageClass;
import com.marcusprado02.commons.ports.files.FileStorePort.UploadOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for GcsFileStoreAdapter covering exception and branch paths. */
@ExtendWith(MockitoExtension.class)
class GcsFileStoreAdapterUnitTest {

  @Mock private Storage storage;
  @Mock private Blob blob;

  private GcsFileStoreAdapter adapter;

  private static final String BUCKET = "my-bucket";
  private static final String KEY = "path/to/file.txt";
  private static final FileId FILE_ID = new FileId(BUCKET, KEY);

  @BeforeEach
  void setUp() {
    adapter = new GcsFileStoreAdapter(storage);
  }

  private void stubBlobMetadata() {
    lenient().when(blob.getContentType()).thenReturn("text/plain");
    lenient().when(blob.getSize()).thenReturn(42L);
    lenient().when(blob.getUpdateTimeOffsetDateTime()).thenReturn(OffsetDateTime.now());
    lenient().when(blob.getEtag()).thenReturn("abc123");
    lenient().when(blob.getMetadata()).thenReturn(Map.of("x", "y"));
  }

  // ── upload ────────────────────────────────────────────────────────────────

  @Test
  void uploadShouldReturnOkWithMinimalOptions() throws IOException {
    when(blob.getSize()).thenReturn(10L);
    when(blob.getEtag()).thenReturn("etag1");
    when(storage.createFrom(any(BlobInfo.class), any(InputStream.class))).thenReturn(blob);

    var result =
        adapter.upload(
            FILE_ID,
            new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)),
            UploadOptions.defaults());

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().fileId()).isEqualTo(FILE_ID);
    assertThat(result.getOrNull().contentLength()).isEqualTo(10L);
  }

  @Test
  void uploadShouldReturnOkWithMetadataAndStorageClass() throws IOException {
    when(blob.getSize()).thenReturn(5L);
    when(blob.getEtag()).thenReturn("etag2");
    when(storage.createFrom(any(BlobInfo.class), any(InputStream.class))).thenReturn(blob);

    var options =
        UploadOptions.builder()
            .contentType("application/json")
            .metadata(Map.of("key", "val"))
            .storageClass(StorageClass.DEEP_ARCHIVE)
            .build();

    var result = adapter.upload(FILE_ID, new ByteArrayInputStream(new byte[5]), options);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void uploadShouldCoverAllStorageClassMappings() throws IOException {
    when(blob.getSize()).thenReturn(1L);
    when(blob.getEtag()).thenReturn("e");
    when(storage.createFrom(any(BlobInfo.class), any(InputStream.class))).thenReturn(blob);

    StorageClass[] classes = {
      StorageClass.STANDARD,
      StorageClass.REDUCED_REDUNDANCY,
      StorageClass.STANDARD_IA,
      StorageClass.ONEZONE_IA,
      StorageClass.INTELLIGENT_TIERING,
      StorageClass.GLACIER,
      StorageClass.GLACIER_IR,
      StorageClass.DEEP_ARCHIVE
    };

    for (var sc : classes) {
      var options = UploadOptions.builder().storageClass(sc).build();
      var result = adapter.upload(FILE_ID, new ByteArrayInputStream(new byte[1]), options);
      assertThat(result.isOk()).as("storage class %s should succeed", sc).isTrue();
    }
  }

  @Test
  void uploadShouldReturnFailOnStorageException() throws IOException {
    when(storage.createFrom(any(BlobInfo.class), any(InputStream.class)))
        .thenThrow(new StorageException(503, "Service Unavailable"));

    var result =
        adapter.upload(FILE_ID, new ByteArrayInputStream(new byte[0]), UploadOptions.defaults());

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to upload file to GCS");
  }

  @Test
  void uploadShouldReturnFailOnIOException() throws IOException {
    when(storage.createFrom(any(BlobInfo.class), any(InputStream.class)))
        .thenThrow(new IOException("disk full"));

    var result =
        adapter.upload(FILE_ID, new ByteArrayInputStream(new byte[0]), UploadOptions.defaults());

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Error reading file content");
  }

  // ── download ──────────────────────────────────────────────────────────────

  @Test
  void downloadShouldReturnOkWithNonNullMetadata() {
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(blob.exists()).thenReturn(true);
    when(blob.getContent()).thenReturn("data".getBytes(StandardCharsets.UTF_8));
    stubBlobMetadata();

    var result = adapter.download(FILE_ID);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().id()).isEqualTo(FILE_ID);
  }

  @Test
  void downloadShouldReturnOkWithNullBlobMetadataField() {
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(blob.exists()).thenReturn(true);
    when(blob.getContent()).thenReturn(new byte[0]);
    when(blob.getContentType()).thenReturn("application/octet-stream");
    when(blob.getSize()).thenReturn(0L);
    when(blob.getUpdateTimeOffsetDateTime()).thenReturn(OffsetDateTime.now());
    when(blob.getEtag()).thenReturn("etag");
    when(blob.getMetadata()).thenReturn(null); // null metadata → falls back to Map.of()

    var result = adapter.download(FILE_ID);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().metadata().customMetadata()).isEmpty();
  }

  @Test
  void downloadShouldReturnFailWhenBlobIsNull() {
    when(storage.get(any(BlobId.class))).thenReturn(null);

    var result = adapter.download(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("File does not exist");
  }

  @Test
  void downloadShouldReturnFailWhenBlobNotExists() {
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(blob.exists()).thenReturn(false);

    var result = adapter.download(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("File does not exist");
  }

  @Test
  void downloadShouldReturnFailOnStorageException() {
    when(storage.get(any(BlobId.class))).thenThrow(new StorageException(500, "internal error"));

    var result = adapter.download(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to download file from GCS");
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  void deleteShouldReturnOkWhenDeleted() {
    when(storage.delete(any(BlobId.class))).thenReturn(true);

    var result = adapter.delete(FILE_ID);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void deleteShouldReturnFailWhenNotFound() {
    when(storage.delete(any(BlobId.class))).thenReturn(false);

    var result = adapter.delete(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("File does not exist");
  }

  @Test
  void deleteShouldReturnFailOnStorageException() {
    when(storage.delete(any(BlobId.class))).thenThrow(new StorageException(403, "Forbidden"));

    var result = adapter.delete(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to delete file from GCS");
  }

  // ── deleteAll ─────────────────────────────────────────────────────────────

  @Test
  void deleteAllShouldReturnDeletedAndFailedCounts() {
    var file1 = new FileId(BUCKET, "a.txt");
    var file2 = new FileId(BUCKET, "b.txt");
    var file3 = new FileId(BUCKET, "c.txt");

    when(storage.delete(BlobId.of(BUCKET, "a.txt"))).thenReturn(true);
    when(storage.delete(BlobId.of(BUCKET, "b.txt"))).thenReturn(false);
    when(storage.delete(BlobId.of(BUCKET, "c.txt"))).thenThrow(new StorageException(500, "error"));

    var result = adapter.deleteAll(List.of(file1, file2, file3));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().deletedCount()).isEqualTo(1);
    assertThat(result.getOrNull().failedDeletes()).containsExactlyInAnyOrder(file2, file3);
  }

  @Test
  void deleteAllShouldReturnAllDeletedWhenAllSucceed() {
    when(storage.delete(any(BlobId.class))).thenReturn(true);

    var result = adapter.deleteAll(List.of(FILE_ID));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().deletedCount()).isEqualTo(1);
    assertThat(result.getOrNull().failedDeletes()).isEmpty();
  }

  // ── exists ────────────────────────────────────────────────────────────────

  @Test
  void existsShouldReturnTrueWhenBlobExistsAndNotNull() {
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(blob.exists()).thenReturn(true);

    var result = adapter.exists(FILE_ID);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isTrue();
  }

  @Test
  void existsShouldReturnFalseWhenBlobIsNull() {
    when(storage.get(any(BlobId.class))).thenReturn(null);

    var result = adapter.exists(FILE_ID);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  @Test
  void existsShouldReturnFalseWhenBlobNotExists() {
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(blob.exists()).thenReturn(false);

    var result = adapter.exists(FILE_ID);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  @Test
  void existsShouldReturnFailOnStorageException() {
    when(storage.get(any(BlobId.class))).thenThrow(new StorageException(503, "unavailable"));

    var result = adapter.exists(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to check if file exists");
  }

  // ── getMetadata ───────────────────────────────────────────────────────────

  @Test
  void getMetadataShouldReturnOk() {
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(blob.exists()).thenReturn(true);
    stubBlobMetadata();

    var result = adapter.getMetadata(FILE_ID);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().contentLength()).isEqualTo(42L);
  }

  @Test
  void getMetadataShouldReturnOkWithNullCustomMetadata() {
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(blob.exists()).thenReturn(true);
    when(blob.getContentType()).thenReturn("text/plain");
    when(blob.getSize()).thenReturn(5L);
    when(blob.getUpdateTimeOffsetDateTime()).thenReturn(OffsetDateTime.now());
    when(blob.getEtag()).thenReturn("e");
    when(blob.getMetadata()).thenReturn(null);

    var result = adapter.getMetadata(FILE_ID);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().customMetadata()).isEmpty();
  }

  @Test
  void getMetadataShouldReturnFailWhenBlobIsNull() {
    when(storage.get(any(BlobId.class))).thenReturn(null);

    var result = adapter.getMetadata(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("File does not exist");
  }

  @Test
  void getMetadataShouldReturnFailWhenBlobNotExists() {
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(blob.exists()).thenReturn(false);

    var result = adapter.getMetadata(FILE_ID);

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void getMetadataShouldReturnFailOnStorageException() {
    when(storage.get(any(BlobId.class))).thenThrow(new StorageException(500, "error"));

    var result = adapter.getMetadata(FILE_ID);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to retrieve file metadata");
  }

  // ── list ──────────────────────────────────────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void listShouldReturnFilesWithNoNextPage() {
    var blob1 = mock(Blob.class);
    var blob2 = mock(Blob.class);
    when(blob1.getName()).thenReturn("prefix/a.txt");
    when(blob2.getName()).thenReturn("prefix/b.txt");

    Page<Blob> page = mock(Page.class);
    when(page.iterateAll()).thenReturn(List.of(blob1, blob2));
    when(page.getNextPageToken()).thenReturn(null);
    when(storage.list(anyString(), any(Storage.BlobListOption[].class))).thenReturn(page);

    var result = adapter.list(BUCKET, "prefix/", ListOptions.defaults());

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().files()).hasSize(2);
    assertThat(result.getOrNull().hasMore()).isFalse();
  }

  @Test
  @SuppressWarnings("unchecked")
  void listShouldReturnNextPageToken() {
    Page<Blob> page = mock(Page.class);
    when(page.iterateAll()).thenReturn(List.of());
    when(page.getNextPageToken()).thenReturn("token123");
    when(storage.list(anyString(), any(Storage.BlobListOption[].class))).thenReturn(page);

    var result = adapter.list(BUCKET, "", ListOptions.defaults());

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().hasMore()).isTrue();
    assertThat(result.getOrNull().continuationToken()).isEqualTo("token123");
  }

  @Test
  @SuppressWarnings("unchecked")
  void listShouldPassContinuationTokenWhenProvided() {
    Page<Blob> page = mock(Page.class);
    when(page.iterateAll()).thenReturn(List.of());
    when(page.getNextPageToken()).thenReturn(null);
    when(storage.list(anyString(), any(Storage.BlobListOption[].class))).thenReturn(page);

    var options = ListOptions.withContinuation("prevToken");
    var result = adapter.list(BUCKET, "", options);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void listShouldReturnFailOnStorageException() {
    when(storage.list(anyString(), any(Storage.BlobListOption[].class)))
        .thenThrow(new StorageException(500, "error"));

    var result = adapter.list(BUCKET, "", ListOptions.defaults());

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to list files");
  }

  // ── generatePresignedUrl ──────────────────────────────────────────────────

  @Test
  void generatePresignedUrlForGetShouldReturnOk() throws Exception {
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(storage.signUrl(any(BlobInfo.class), anyLong(), any(TimeUnit.class), any()))
        .thenReturn(new URL("https://storage.googleapis.com/bucket/key"));

    var result = adapter.generatePresignedUrl(FILE_ID, PresignedOperation.GET, Duration.ofHours(1));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().toString()).contains("storage.googleapis.com");
  }

  @Test
  void generatePresignedUrlForGetShouldReturnFailWhenBlobNotFound() {
    when(storage.get(any(BlobId.class))).thenReturn(null);

    var result = adapter.generatePresignedUrl(FILE_ID, PresignedOperation.GET, Duration.ofHours(1));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Cannot generate GET URL");
  }

  @Test
  void generatePresignedUrlForPutShouldSucceedWhenBlobNotFound() throws Exception {
    when(storage.get(any(BlobId.class))).thenReturn(null);
    when(storage.signUrl(any(BlobInfo.class), anyLong(), any(TimeUnit.class), any()))
        .thenReturn(new URL("https://storage.googleapis.com/bucket/key"));

    var result =
        adapter.generatePresignedUrl(FILE_ID, PresignedOperation.PUT, Duration.ofMinutes(15));

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generatePresignedUrlForPutShouldSucceedWhenBlobExists() throws Exception {
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(storage.signUrl(any(BlobInfo.class), anyLong(), any(TimeUnit.class), any()))
        .thenReturn(new URL("https://storage.googleapis.com/bucket/key"));

    var result =
        adapter.generatePresignedUrl(FILE_ID, PresignedOperation.PUT, Duration.ofMinutes(15));

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generatePresignedUrlShouldReturnFailOnStorageException() {
    when(storage.get(any(BlobId.class))).thenThrow(new StorageException(403, "forbidden"));

    var result = adapter.generatePresignedUrl(FILE_ID, PresignedOperation.GET, Duration.ofHours(1));

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to generate signed URL");
  }

  // ── copy ──────────────────────────────────────────────────────────────────

  @Test
  void copyShouldReturnOkWhenSourceExists() {
    var source = new FileId(BUCKET, "src.txt");
    var dest = new FileId(BUCKET, "dst.txt");

    when(storage.get(BlobId.of(BUCKET, "src.txt"))).thenReturn(blob);
    when(blob.exists()).thenReturn(true);
    when(storage.copy(any(Storage.CopyRequest.class))).thenReturn(null);

    var result = adapter.copy(source, dest);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void copyShouldReturnFailWhenSourceBlobIsNull() {
    var source = new FileId(BUCKET, "missing.txt");
    var dest = new FileId(BUCKET, "dst.txt");

    when(storage.get(BlobId.of(BUCKET, "missing.txt"))).thenReturn(null);

    var result = adapter.copy(source, dest);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Source file does not exist");
  }

  @Test
  void copyShouldReturnFailWhenSourceBlobNotExists() {
    var source = new FileId(BUCKET, "missing.txt");
    var dest = new FileId(BUCKET, "dst.txt");

    when(storage.get(BlobId.of(BUCKET, "missing.txt"))).thenReturn(blob);
    when(blob.exists()).thenReturn(false);

    var result = adapter.copy(source, dest);

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void copyShouldReturnFailOnStorageException() {
    var source = new FileId(BUCKET, "src.txt");
    var dest = new FileId(BUCKET, "dst.txt");

    when(storage.get(BlobId.of(BUCKET, "src.txt")))
        .thenThrow(new StorageException(500, "internal error"));

    var result = adapter.copy(source, dest);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Failed to copy file");
  }
}
