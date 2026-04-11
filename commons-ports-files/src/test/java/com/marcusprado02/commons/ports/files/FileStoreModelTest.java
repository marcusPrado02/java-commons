package com.marcusprado02.commons.ports.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for file store port domain model. */
class FileStoreModelTest {

  // -----------------------------------------------------------------------
  // FileObject
  // -----------------------------------------------------------------------

  @Test
  void fileObject_createsSuccessfully() {
    FileId id = new FileId("bucket", "key/file.txt");
    ByteArrayInputStream stream = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
    FileObject.FileMetadata meta =
        FileObject.FileMetadata.builder().contentType("text/plain").contentLength(4L).build();

    FileObject obj = new FileObject(id, stream, meta);
    assertEquals(id, obj.id());
    assertNotNull(obj.content());
    assertNotNull(obj.metadata());
  }

  @Test
  void fileObject_nullIdThrows() {
    ByteArrayInputStream stream = new ByteArrayInputStream(new byte[0]);
    FileObject.FileMetadata meta = FileObject.FileMetadata.builder().build();
    assertThrows(NullPointerException.class, () -> new FileObject(null, stream, meta));
  }

  @Test
  void fileObject_nullContentThrows() {
    FileId id = new FileId("b", "k");
    FileObject.FileMetadata meta = FileObject.FileMetadata.builder().build();
    assertThrows(NullPointerException.class, () -> new FileObject(id, null, meta));
  }

  @Test
  void fileObject_nullMetadataThrows() {
    FileId id = new FileId("b", "k");
    ByteArrayInputStream stream = new ByteArrayInputStream(new byte[0]);
    assertThrows(NullPointerException.class, () -> new FileObject(id, stream, null));
  }

  // -----------------------------------------------------------------------
  // FileMetadata
  // -----------------------------------------------------------------------

  @Test
  void fileMetadata_builder_defaults() {
    FileObject.FileMetadata meta = FileObject.FileMetadata.builder().build();
    assertEquals("application/octet-stream", meta.contentType());
    assertEquals(0L, meta.contentLength());
    assertNull(meta.lastModified());
    assertNull(meta.etag());
    assertTrue(meta.customMetadata().isEmpty());
  }

  @Test
  void fileMetadata_builder_allFields() {
    Instant now = Instant.now();
    FileObject.FileMetadata meta =
        FileObject.FileMetadata.builder()
            .contentType("application/json")
            .contentLength(100L)
            .lastModified(now)
            .etag("\"abc123\"")
            .customMetadata(Map.of("x-tag", "value"))
            .build();

    assertEquals("application/json", meta.contentType());
    assertEquals(100L, meta.contentLength());
    assertEquals(now, meta.lastModified());
    assertEquals("\"abc123\"", meta.etag());
    assertEquals("value", meta.customMetadata().get("x-tag"));
  }

  @Test
  void fileMetadata_addCustomMetadata() {
    FileObject.FileMetadata meta =
        FileObject.FileMetadata.builder().addCustomMetadata("key", "val").build();
    assertEquals("val", meta.customMetadata().get("key"));
  }

  @Test
  void fileMetadata_nullCustomMetadataBecomesEmpty() {
    FileObject.FileMetadata meta = new FileObject.FileMetadata("text/plain", 0L, null, null, null);
    assertTrue(meta.customMetadata().isEmpty());
  }

  @Test
  void fileMetadata_nullContentTypeThrows() {
    assertThrows(
        NullPointerException.class, () -> new FileObject.FileMetadata(null, 0L, null, null, null));
  }

  // -----------------------------------------------------------------------
  // FileStorePort inner records
  // -----------------------------------------------------------------------

  @Test
  void uploadResult_createsSuccessfully() {
    FileId id = new FileId("bucket", "key");
    FileStorePort.UploadResult result = new FileStorePort.UploadResult(id, "etag", 100L);
    assertEquals(id, result.fileId());
    assertEquals("etag", result.etag());
    assertEquals(100L, result.contentLength());
  }

  @Test
  void uploadResult_nullFileIdThrows() {
    assertThrows(
        NullPointerException.class, () -> new FileStorePort.UploadResult(null, "etag", 10L));
  }

  @Test
  void deleteResult_createsSuccessfully() {
    FileStorePort.DeleteResult result = new FileStorePort.DeleteResult(3, List.of());
    assertEquals(3, result.deletedCount());
    assertTrue(result.failedDeletes().isEmpty());
  }

  @Test
  void deleteResult_withFailures() {
    FileId failed = new FileId("b", "k");
    FileStorePort.DeleteResult result = new FileStorePort.DeleteResult(2, List.of(failed));
    assertEquals(2, result.deletedCount());
    assertEquals(1, result.failedDeletes().size());
  }

  @Test
  void listResult_createsSuccessfully() {
    FileId f = new FileId("bucket", "key");
    FileStorePort.ListResult result = new FileStorePort.ListResult(List.of(f), null, false);
    assertEquals(1, result.files().size());
    assertNull(result.continuationToken());
    assertFalse(result.hasMore());
  }

  // -----------------------------------------------------------------------
  // UploadOptions
  // -----------------------------------------------------------------------

  @Test
  void uploadOptions_defaults() {
    FileStorePort.UploadOptions opts = FileStorePort.UploadOptions.defaults();
    assertEquals("application/octet-stream", opts.contentType());
    assertEquals(FileStorePort.StorageClass.STANDARD, opts.storageClass());
    assertNull(opts.encryption());
  }

  @Test
  void uploadOptions_builder() {
    FileStorePort.UploadOptions opts =
        FileStorePort.UploadOptions.builder()
            .contentType("image/png")
            .storageClass(FileStorePort.StorageClass.GLACIER)
            .encryption(FileStorePort.ServerSideEncryption.AES256)
            .metadata(Map.of("author", "test"))
            .build();

    assertEquals("image/png", opts.contentType());
    assertEquals(FileStorePort.StorageClass.GLACIER, opts.storageClass());
    assertEquals(FileStorePort.ServerSideEncryption.AES256, opts.encryption());
  }

  // -----------------------------------------------------------------------
  // ListOptions
  // -----------------------------------------------------------------------

  @Test
  void listOptions_defaults() {
    FileStorePort.ListOptions opts = FileStorePort.ListOptions.defaults();
    assertEquals(1000, opts.maxKeys());
    assertNull(opts.continuationToken());
  }

  @Test
  void listOptions_withMaxKeys() {
    FileStorePort.ListOptions opts = FileStorePort.ListOptions.withMaxKeys(50);
    assertEquals(50, opts.maxKeys());
  }

  @Test
  void listOptions_withContinuation() {
    FileStorePort.ListOptions opts = FileStorePort.ListOptions.withContinuation("token123");
    assertEquals("token123", opts.continuationToken());
  }

  // -----------------------------------------------------------------------
  // Enums
  // -----------------------------------------------------------------------

  @Test
  void storageClass_allValues() {
    assertEquals(8, FileStorePort.StorageClass.values().length);
  }

  @Test
  void serverSideEncryption_allValues() {
    assertEquals(2, FileStorePort.ServerSideEncryption.values().length);
  }

  @Test
  void presignedOperation_allValues() {
    assertEquals(2, FileStorePort.PresignedOperation.values().length);
  }
}
