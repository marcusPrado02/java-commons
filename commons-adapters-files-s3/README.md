# Commons Adapters Files S3

AWS S3 adapter implementation for the `FileStorePort` interface.

## Overview

This module provides a production-ready Amazon S3 adapter for file storage operations. It implements all methods defined in the `FileStorePort` interface using the AWS SDK v2.

## Features

- ✅ **Upload/Download**: Full support for uploading and downloading files
- ✅ **Multipart Support**: Automatic handling of large files with multipart uploads
- ✅ **Presigned URLs**: Generate temporary URLs for GET and PUT operations
- ✅ **Metadata Management**: Store and retrieve custom file metadata
- ✅ **Batch Operations**: Efficient batch delete operations
- ✅ **Copy Operations**: Server-side file copying
- ✅ **Storage Classes**: Support for all S3 storage classes (Standard, IA, Glacier, etc.)
- ✅ **Encryption**: Server-side encryption with AES256, AWS KMS
- ✅ **LocalStack Testing**: Full test suite using Testcontainers and LocalStack

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-files-s3</artifactId>
  <version>${commons.version}</version>
</dependency>
```

## Usage

### Basic Setup

```java
import com.marcusprado02.commons.adapters.files.s3.*;
import com.marcusprado02.commons.ports.files.*;

// Create configuration
S3Configuration config = S3Configuration.defaults("us-east-1");

// Create adapter
S3FileStoreAdapter adapter = S3ClientFactory.createAdapter(config);
```

### Upload a File

```java
FileId fileId = new FileId("my-bucket", "documents/report.pdf");
InputStream content = new FileInputStream("report.pdf");

UploadOptions options = UploadOptions.builder()
    .contentType("application/pdf")
    .metadata(Map.of("author", "John Doe"))
    .storageClass(StorageClass.STANDARD_IA)
    .build();

Result<UploadResult> result = adapter.upload(fileId, content, options);

if (result.isOk()) {
    UploadResult uploadResult = result.getOrNull();
    System.out.println("Uploaded: " + uploadResult.etag());
}
```

### Download a File

```java
FileId fileId = new FileId("my-bucket", "documents/report.pdf");

Result<FileObject> result = adapter.download(fileId);

if (result.isOk()) {
    FileObject file = result.getOrNull();
    InputStream content = file.content();
    FileMetadata metadata = file.metadata();
    
    // Read content
    byte[] bytes = content.readAllBytes();
}
```

### Generate Presigned URL

```java
FileId fileId = new FileId("my-bucket", "shared/file.txt");
Duration validity = Duration.ofHours(1);

// For downloads
Result<URL> getUrl = adapter.generatePresignedUrl(
    fileId, 
    PresignedOperation.GET, 
    validity
);

// For uploads
Result<URL> putUrl = adapter.generatePresignedUrl(
    fileId, 
    PresignedOperation.PUT, 
    validity
);
```

### List Files

```java
String bucket = "my-bucket";
String prefix = "documents/2024/";

Result<ListResult> result = adapter.list(bucket, prefix);

if (result.isOk()) {
    ListResult listResult = result.getOrNull();
    
    for (FileId fileId : listResult.files()) {
        System.out.println(fileId.toPath());
    }
    
    // Pagination
    if (listResult.hasMore()) {
        String token = listResult.continuationToken();
        ListOptions nextPage = ListOptions.withContinuation(token);
        adapter.list(bucket, prefix, nextPage);
    }
}
```

### Delete Files

```java
// Single file
FileId fileId = new FileId("my-bucket", "old-file.txt");
Result<Void> result = adapter.delete(fileId);

// Multiple files
List<FileId> fileIds = List.of(
    new FileId("my-bucket", "file1.txt"),
    new FileId("my-bucket", "file2.txt"),
    new FileId("my-bucket", "file3.txt")
);

Result<DeleteResult> batchResult = adapter.deleteAll(fileIds);

if (batchResult.isOk()) {
    DeleteResult deleteResult = batchResult.getOrNull();
    System.out.println("Deleted: " + deleteResult.deletedCount());
    System.out.println("Failed: " + deleteResult.failedDeletes().size());
}
```

### Copy Files

```java
FileId source = new FileId("my-bucket", "original/file.txt");
FileId destination = new FileId("my-bucket", "backup/file.txt");

Result<Void> result = adapter.copy(source, destination);
```

## Configuration

### S3Configuration Options

```java
S3Configuration config = S3Configuration.builder()
    .region("us-east-1")                           // AWS region
    .endpoint("https://custom-endpoint.com")       // Custom endpoint (optional)
    .pathStyleAccessEnabled(false)                 // Path-style access (for LocalStack)
    .multipartThresholdBytes(5 * 1024 * 1024)     // 5 MB threshold
    .multipartChunkSizeBytes(5 * 1024 * 1024)     // 5 MB chunk size
    .build();
```

### LocalStack Configuration

For local development and testing:

```java
S3Configuration config = S3Configuration.localStack("http://localhost:4566");
```

### Custom Credentials

```java
import software.amazon.awssdk.auth.credentials.*;

AwsCredentialsProvider credentials = StaticCredentialsProvider.create(
    AwsBasicCredentials.create("accessKey", "secretKey")
);

S3Client client = S3ClientFactory.createClient(config, credentials);
S3Presigner presigner = S3ClientFactory.createPresigner(config, credentials);

S3FileStoreAdapter adapter = new S3FileStoreAdapter(client, presigner, config);
```

## Storage Classes

Supported S3 storage classes:

- `STANDARD` - Standard storage (default)
- `REDUCED_REDUNDANCY` - Reduced redundancy
- `STANDARD_IA` - Infrequent Access
- `ONEZONE_IA` - One Zone Infrequent Access
- `INTELLIGENT_TIERING` - Intelligent tiering
- `GLACIER` - Glacier
- `GLACIER_IR` - Glacier Instant Retrieval
- `DEEP_ARCHIVE` - Deep Archive

## Encryption

Supported server-side encryption options:

- `AES256` - S3-managed encryption
- `AWS_KMS` - KMS-managed encryption
- `AWS_KMS_DSSE` - KMS with DSSE

## Error Handling

All operations return `Result<T>` for type-safe error handling:

```java
Result<FileObject> result = adapter.download(fileId);

result.fold(
    file -> {
        // Success case
        System.out.println("Downloaded: " + file.id());
        return null;
    },
    problem -> {
        // Error case
        System.err.println("Error: " + problem.title());
        System.err.println("Detail: " + problem.detail());
        return null;
    }
);
```

Common error scenarios:

- **File Not Found**: Non-existent file in S3
- **S3 Upload Error**: Upload failures (permissions, network, etc.)
- **S3 Download Error**: Download failures
- **I/O Error**: Local I/O issues when reading content

## Testing

The module includes comprehensive tests using Testcontainers and LocalStack:

```java
@Testcontainers
class S3FileStoreAdapterTest {
  
  @Container
  static LocalStackContainer localstack = new LocalStackContainer(
      DockerImageName.parse("localstack/localstack:3.0")
  ).withServices(S3);
  
  @Test
  void shouldUploadFile() {
    // Test implementation
  }
}
```

Run tests with:

```bash
mvn test
```

## Dependencies

- AWS SDK for Java 2.x (S3)
- Commons Kernel Result
- Commons Ports Files
- SLF4J for logging
- Testcontainers (LocalStack) for testing

## Architecture

This adapter follows the **Hexagonal Architecture** pattern:

```
Application Layer
      ↓
FileStorePort (Port - Interface)
      ↓
S3FileStoreAdapter (Adapter - Implementation)
      ↓
AWS S3 SDK (External Service)
```

## Performance Considerations

1. **Multipart Upload**: Files larger than 5 MB automatically use multipart upload
2. **Streaming**: Download operations use streaming to minimize memory usage
3. **Batch Operations**: Use `deleteAll()` for efficient batch deletions
4. **Connection Pooling**: AWS SDK handles connection pooling automatically

## Best Practices

1. **Close Streams**: Always close input streams after use
2. **Use Result**: Handle errors using the Result type
3. **Presigned URLs**: Set reasonable expiration times (< 7 days)
4. **Storage Classes**: Choose appropriate storage class for your use case
5. **Metadata**: Use custom metadata for searchable attributes
6. **Testing**: Use LocalStack for local development and CI/CD

## Example: Complete Workflow

```java
public class FileUploadService {
  
  private final FileStorePort fileStore;
  
  public FileUploadService(FileStorePort fileStore) {
    this.fileStore = fileStore;
  }
  
  public Result<FileId> uploadDocument(String bucket, InputStream content, String fileName) {
    // Generate unique file ID
    FileId fileId = FileId.generate(bucket, "documents/");
    
    // Set upload options
    UploadOptions options = UploadOptions.builder()
        .contentType(detectContentType(fileName))
        .metadata(Map.of(
            "originalName", fileName,
            "uploadedAt", Instant.now().toString()
        ))
        .storageClass(StorageClass.STANDARD)
        .build();
    
    // Upload file
    return fileStore.upload(fileId, content, options)
        .map(UploadResult::fileId);
  }
  
  private String detectContentType(String fileName) {
    return URLConnection.guessContentTypeFromName(fileName);
  }
}
```

## License

This module is part of the Commons library and follows the same license.

## Support

For issues, questions, or contributions, please refer to the main Commons library repository.
