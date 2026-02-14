package com.marcusprado02.commons.ports.files;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileIdTest {

  @Test
  @DisplayName("Should create FileId with valid bucket and key")
  void shouldCreateFileId() {
    // When
    FileId fileId = new FileId("my-bucket", "folder/file.txt");

    // Then
    assertThat(fileId.bucket()).isEqualTo("my-bucket");
    assertThat(fileId.key()).isEqualTo("folder/file.txt");
  }

  @Test
  @DisplayName("Should reject null bucket")
  void shouldRejectNullBucket() {
    assertThatThrownBy(() -> new FileId(null, "key"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("bucket must not be null");
  }

  @Test
  @DisplayName("Should reject null key")
  void shouldRejectNullKey() {
    assertThatThrownBy(() -> new FileId("bucket", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("key must not be null");
  }

  @Test
  @DisplayName("Should reject blank bucket")
  void shouldRejectBlankBucket() {
    assertThatThrownBy(() -> new FileId("", "key"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bucket must not be blank");
  }

  @Test
  @DisplayName("Should reject blank key")
  void shouldRejectBlankKey() {
    assertThatThrownBy(() -> new FileId("bucket", ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("key must not be blank");
  }

  @Test
  @DisplayName("Should generate FileId with UUID")
  void shouldGenerateFileId() {
    // When
    FileId fileId = FileId.generate("my-bucket", "documents/");

    // Then
    assertThat(fileId.bucket()).isEqualTo("my-bucket");
    assertThat(fileId.key()).startsWith("documents/");
    assertThat(fileId.key()).hasSize("documents/".length() + 36); // UUID length
  }

  @Test
  @DisplayName("Should generate FileId without trailing slash")
  void shouldGenerateFileIdWithoutTrailingSlash() {
    // When
    FileId fileId = FileId.generate("my-bucket", "documents");

    // Then
    assertThat(fileId.bucket()).isEqualTo("my-bucket");
    assertThat(fileId.key()).startsWith("documents/");
  }

  @Test
  @DisplayName("Should create FileId from path")
  void shouldCreateFromPath() {
    // When
    FileId fileId = FileId.fromPath("my-bucket/folder/file.txt");

    // Then
    assertThat(fileId.bucket()).isEqualTo("my-bucket");
    assertThat(fileId.key()).isEqualTo("folder/file.txt");
  }

  @Test
  @DisplayName("Should reject invalid path format - no slash")
  void shouldRejectInvalidPathNoSlash() {
    assertThatThrownBy(() -> FileId.fromPath("mybucket"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid path format");
  }

  @Test
  @DisplayName("Should reject invalid path format - slash at start")
  void shouldRejectInvalidPathSlashAtStart() {
    assertThatThrownBy(() -> FileId.fromPath("/bucket/key"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid path format");
  }

  @Test
  @DisplayName("Should reject invalid path format - slash at end only")
  void shouldRejectInvalidPathSlashAtEndOnly() {
    assertThatThrownBy(() -> FileId.fromPath("bucket/"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid path format");
  }

  @Test
  @DisplayName("Should convert to path")
  void shouldConvertToPath() {
    // Given
    FileId fileId = new FileId("my-bucket", "folder/file.txt");

    // When
    String path = fileId.toPath();

    // Then
    assertThat(path).isEqualTo("my-bucket/folder/file.txt");
  }

  @Test
  @DisplayName("Should get file extension")
  void shouldGetExtension() {
    // Given
    FileId fileId = new FileId("bucket", "folder/file.txt");

    // When
    String extension = fileId.getExtension();

    // Then
    assertThat(extension).isEqualTo("txt");
  }

  @Test
  @DisplayName("Should return empty extension for no extension")
  void shouldReturnEmptyExtensionForNoExtension() {
    // Given
    FileId fileId = new FileId("bucket", "folder/file");

    // When
    String extension = fileId.getExtension();

    // Then
    assertThat(extension).isEmpty();
  }

  @Test
  @DisplayName("Should get file name")
  void shouldGetFileName() {
    // Given
    FileId fileId = new FileId("bucket", "folder/subfolder/file.txt");

    // When
    String fileName = fileId.getFileName();

    // Then
    assertThat(fileName).isEqualTo("file.txt");
  }

  @Test
  @DisplayName("Should get file name without folder")
  void shouldGetFileNameWithoutFolder() {
    // Given
    FileId fileId = new FileId("bucket", "file.txt");

    // When
    String fileName = fileId.getFileName();

    // Then
    assertThat(fileName).isEqualTo("file.txt");
  }

  @Test
  @DisplayName("Should convert to string")
  void shouldConvertToString() {
    // Given
    FileId fileId = new FileId("my-bucket", "folder/file.txt");

    // When
    String string = fileId.toString();

    // Then
    assertThat(string).isEqualTo("my-bucket/folder/file.txt");
  }

  @Test
  @DisplayName("Should be equal when bucket and key are equal")
  void shouldBeEqual() {
    // Given
    FileId fileId1 = new FileId("bucket", "key");
    FileId fileId2 = new FileId("bucket", "key");

    // Then
    assertThat(fileId1).isEqualTo(fileId2);
    assertThat(fileId1.hashCode()).isEqualTo(fileId2.hashCode());
  }

  @Test
  @DisplayName("Should not be equal when bucket differs")
  void shouldNotBeEqualWhenBucketDiffers() {
    // Given
    FileId fileId1 = new FileId("bucket1", "key");
    FileId fileId2 = new FileId("bucket2", "key");

    // Then
    assertThat(fileId1).isNotEqualTo(fileId2);
  }

  @Test
  @DisplayName("Should not be equal when key differs")
  void shouldNotBeEqualWhenKeyDiffers() {
    // Given
    FileId fileId1 = new FileId("bucket", "key1");
    FileId fileId2 = new FileId("bucket", "key2");

    // Then
    assertThat(fileId1).isNotEqualTo(fileId2);
  }
}
