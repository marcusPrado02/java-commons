# Commons Adapters - Azure Blob Storage

Azure Blob Storage implementation of the FileStorePort for secure and scalable cloud file storage.

## Overview

This module provides an Azure Blob Storage adapter that implements the `FileStorePort` interface, enabling seamless integration with Azure Blob Storage for file operations. It supports multiple authentication methods, including connection strings, SAS tokens, and Managed Identity.

## Features

- ✅ **Full FileStorePort Implementation**: Upload, download, delete, list, copy, and metadata operations
- ✅ **Multiple Authentication Methods**: Connection String, SAS Token, and Managed Identity
- ✅ **Storage Tiers**: Support for Hot, Cool, and Archive access tiers
- ✅ **Presigned URLs**: Generate SAS URLs for temporary access (GET/PUT operations)
- ✅ **Batch Operations**: Delete multiple files efficiently
- ✅ **Custom Metadata**: Attach custom key-value metadata to files
- ✅ **Type-Safe Results**: Uses Result<T> pattern for error handling
- ✅ **Local Testing**: Full Azurite support for local development
- ✅ **Comprehensive Logging**: SLF4J integration with detailed operation logging

## Installation

### Maven

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-files-azure-blob</artifactId>
    <version>${commons.version}</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.marcusprado02.commons:commons-adapters-files-azure-blob:${commonsVersion}'
```

## Quick Start

### Basic Usage with Connection String

```java
// Create configuration
AzureBlobConfiguration config = AzureBlobConfiguration.withConnectionString(
    "DefaultEndpointsProtocol=https;AccountName=myaccount;AccountKey=...;EndpointSuffix=core.windows.net"
);

// Create adapter
AzureBlobFileStoreAdapter adapter = AzureBlobClientFactory.createAdapter(config);

// Upload a file
FileId fileId = new FileId("my-container", "documents/report.pdf");
InputStream content = new FileInputStream("report.pdf");

UploadOptions options = UploadOptions.builder()
    .contentType("application/pdf")
    .build();

Result<UploadResult> result = adapter.upload(fileId, content, options);

if (result.isSuccess()) {
    System.out.println("File uploaded: " + result.get().etag());
}
```

### Using Managed Identity (Recommended for Azure)

```java
// Configure with Managed Identity
AzureBlobConfiguration config = AzureBlobConfiguration.withManagedIdentity(
    "https://myaccount.blob.core.windows.net",
    "myaccount"
);

AzureBlobFileStoreAdapter adapter = AzureBlobClientFactory.createAdapter(config);
```

### Using SAS Token

```java
// Configure with SAS token
AzureBlobConfiguration config = AzureBlobConfiguration.withSasToken(
    "https://myaccount.blob.core.windows.net",
    "sp=racwdl&st=2024-01-01T00:00:00Z&se=2024-12-31T23:59:59Z&sv=2022-11-02&sr=c&sig=..."
);

AzureBlobFileStoreAdapter adapter = AzureBlobClientFactory.createAdapter(config);
```

## Configuration

### Authentication Methods

#### 1. Connection String Authentication

Best for development and testing.

```java
AzureBlobConfiguration config = AzureBlobConfiguration.withConnectionString(
    "DefaultEndpointsProtocol=https;AccountName=myaccount;AccountKey=...;EndpointSuffix=core.windows.net"
);
```

#### 2. SAS Token Authentication

Best for limited-time access with specific permissions.

```java
AzureBlobConfiguration config = AzureBlobConfiguration.withSasToken(
    "https://myaccount.blob.core.windows.net",
    "sp=racwdl&st=2024-01-01T00:00:00Z&se=2024-12-31T23:59:59Z&sv=2022-11-02&sr=c&sig=..."
);
```

#### 3. Managed Identity Authentication (Recommended)

Best for production Azure environments - no credentials in code.

```java
AzureBlobConfiguration config = AzureBlobConfiguration.withManagedIdentity(
    "https://myaccount.blob.core.windows.net",
    "myaccount"
);
```

### Local Testing with Azurite

```java
AzureBlobConfiguration config = AzureBlobConfiguration.azurite(
    "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;"
);
```

### Custom Configuration

```java
AzureBlobConfiguration config = AzureBlobConfiguration.builder()
    .endpoint("https://myaccount.blob.core.windows.net")
    .accountName("myaccount")
    .authenticationType(AuthenticationType.MANAGED_IDENTITY)
    .useManagedIdentity(true)
    .maxRetries(5)
    .timeoutSeconds(120)
    .build();
```

## Core Operations

### Upload File

```java
FileId fileId = new FileId("my-container", "images/photo.jpg");
InputStream content = new FileInputStream("photo.jpg");

UploadOptions options = UploadOptions.builder()
    .contentType("image/jpeg")
    .metadata(Map.of(
        "author", "John Doe",
        "category", "photos"
    ))
    .storageClass(StorageClass.STANDARD)
    .build();

Result<UploadResult> result = adapter.upload(fileId, content, options);

result.ifSuccess(uploadResult -> {
    System.out.println("ETag: " + uploadResult.etag());
    System.out.println("Size: " + uploadResult.size());
});
```

### Download File

```java
FileId fileId = new FileId("my-container", "documents/report.pdf");

Result<FileObject> result = adapter.download(fileId);

result.ifSuccess(fileObject -> {
    try (InputStream content = fileObject.content()) {
        Files.copy(content, Path.of("downloaded-report.pdf"));
        System.out.println("Content-Type: " + fileObject.metadata().contentType());
        System.out.println("Size: " + fileObject.metadata().contentLength());
    }
});
```

### Check File Existence

```java
FileId fileId = new FileId("my-container", "data/file.json");

Result<Boolean> result = adapter.exists(fileId);

result.ifSuccess(exists -> {
    System.out.println("File exists: " + exists);
});
```

### Get File Metadata

```java
FileId fileId = new FileId("my-container", "documents/report.pdf");

Result<FileMetadata> result = adapter.getMetadata(fileId);

result.ifSuccess(metadata -> {
    System.out.println("Content-Type: " + metadata.contentType());
    System.out.println("Size: " + metadata.contentLength());
    System.out.println("Last Modified: " + metadata.lastModified());
    System.out.println("ETag: " + metadata.etag());
    System.out.println("Custom Metadata: " + metadata.customMetadata());
});
```

### List Files

```java
String container = "my-container";
String prefix = "documents/2024/";
ListOptions options = new ListOptions(100); // max 100 results

Result<ListResult> result = adapter.list(container, prefix, options);

result.ifSuccess(listResult -> {
    listResult.files().forEach(fileId -> {
        System.out.println("File: " + fileId.key());
    });
});
```

### Generate Presigned URL (SAS URL)

#### For Download (GET)

```java
FileId fileId = new FileId("my-container", "public/document.pdf");
Duration validity = Duration.ofHours(1);

Result<URL> result = adapter.generatePresignedUrl(fileId, PresignedOperation.GET, validity);

result.ifSuccess(url -> {
    System.out.println("Download URL (valid for 1 hour): " + url);
    // Share this URL for temporary download access
});
```

#### For Upload (PUT)

```java
FileId fileId = new FileId("my-container", "uploads/new-file.jpg");
Duration validity = Duration.ofMinutes(30);

Result<URL> result = adapter.generatePresignedUrl(fileId, PresignedOperation.PUT, validity);

result.ifSuccess(url -> {
    System.out.println("Upload URL (valid for 30 minutes): " + url);
    // Use this URL to upload directly to Azure Blob
});
```

### Copy File

```java
FileId source = new FileId("my-container", "originals/photo.jpg");
FileId destination = new FileId("my-container", "backups/photo-backup.jpg");

Result<Void> result = adapter.copy(source, destination);

result.ifSuccess(() -> {
    System.out.println("File copied successfully");
});
```

### Delete File

```java
FileId fileId = new FileId("my-container", "temp/old-file.txt");

Result<Void> result = adapter.delete(fileId);

result.ifSuccess(() -> {
    System.out.println("File deleted successfully");
});
```

### Delete Multiple Files

```java
List<FileId> files = List.of(
    new FileId("my-container", "temp/file1.txt"),
    new FileId("my-container", "temp/file2.txt"),
    new FileId("my-container", "temp/file3.txt")
);

Result<DeleteResult> result = adapter.deleteAll(files);

result.ifSuccess(deleteResult -> {
    System.out.println("Deleted: " + deleteResult.deletedCount());
    System.out.println("Failed: " + deleteResult.failedDeletes().size());
});
```

## Storage Tiers

Azure Blob Storage supports different access tiers for cost optimization:

```java
UploadOptions options = UploadOptions.builder()
    .storageClass(StorageClass.STANDARD)     // Hot tier (frequent access)
    .storageClass(StorageClass.STANDARD_IA)  // Cool tier (infrequent access)
    .storageClass(StorageClass.GLACIER)      // Archive tier (rare access)
    .build();
```

### Tier Mapping

| FileStorePort | Azure Blob | Use Case |
|--------------|------------|----------|
| `STANDARD` | Hot | Frequently accessed data |
| `STANDARD_IA` | Cool | Infrequently accessed data (>30 days) |
| `GLACIER` | Archive | Rarely accessed data (>180 days) |
| `DEEP_ARCHIVE` | Archive | Long-term archival |

## Error Handling

All operations return `Result<T>` for type-safe error handling:

```java
Result<UploadResult> result = adapter.upload(fileId, content, options);

result
    .ifSuccess(uploadResult -> {
        System.out.println("Upload successful: " + uploadResult.etag());
    })
    .ifFailure(problem -> {
        System.err.println("Upload failed: " + problem.message());
        System.err.println("Error code: " + problem.errorCode().code());
        System.err.println("Category: " + problem.category());
    });
```

### Common Error Codes

| Error Code | Description |
|-----------|-------------|
| `FILE_NOT_FOUND` | Requested blob does not exist |
| `AZURE_UPLOAD_ERROR` | Failed to upload blob to Azure |
| `AZURE_DOWNLOAD_ERROR` | Failed to download blob from Azure |
| `AZURE_DELETE_ERROR` | Failed to delete blob from Azure |
| `AZURE_SAS_ERROR` | Failed to generate SAS URL |
| `AZURE_COPY_ERROR` | Failed to copy blob |
| `IO_ERROR` | I/O error reading file content |

## Testing

### Local Testing with Azurite

1. Start Azurite container:

```bash
docker run -p 10000:10000 mcr.microsoft.com/azure-storage/azurite:latest \
    azurite-blob --blobHost 0.0.0.0 --blobPort 10000
```

2. Use Azurite configuration:

```java
AzureBlobConfiguration config = AzureBlobConfiguration.azurite(
    "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;"
);
```

### Integration Tests

The module includes comprehensive integration tests using Testcontainers:

```java
@Testcontainers
class AzureBlobFileStoreAdapterTest {
  
  @Container
  private static final GenericContainer<?> azuriteContainer = new GenericContainer<>(
      DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:latest"))
      .withCommand("azurite-blob", "--blobHost", "0.0.0.0")
      .withExposedPorts(10000);
  
  @Test
  void shouldUploadAndDownload() {
    // Test implementation
  }
}
```

Run tests:

```bash
mvn test
```

## Performance Considerations

### Multipart Uploads

Azure Blob automatically handles large file uploads using block blobs. The SDK manages chunking internally.

### Batch Operations

Use `deleteAll()` for deleting multiple files:

```java
// Efficient batch delete
adapter.deleteAll(List.of(file1, file2, file3));

// Less efficient
files.forEach(adapter::delete);
```

### Connection Pooling

The Azure SDK manages HTTP connection pooling automatically. Reuse the same `BlobServiceClient` instance across requests.

### Buffering

Azure Blob requires the full content length upfront. The adapter reads the entire stream into memory:

```java
// For large files, consider streaming optimizations
byte[] bytes = content.readAllBytes();
```

## Best Practices

### 1. Use Managed Identity in Production

```java
// ✅ Recommended for Azure environments
AzureBlobConfiguration config = AzureBlobConfiguration.withManagedIdentity(
    "https://myaccount.blob.core.windows.net",
    "myaccount"
);
```

### 2. Set Appropriate Retry Policies

```java
AzureBlobConfiguration config = AzureBlobConfiguration.builder()
    .endpoint("https://myaccount.blob.core.windows.net")
    .accountName("myaccount")
    .authenticationType(AuthenticationType.MANAGED_IDENTITY)
    .useManagedIdentity(true)
    .maxRetries(5)  // Retry transient failures
    .timeoutSeconds(120)  // Reasonable timeout
    .build();
```

### 3. Use Descriptive File Keys

```java
// ✅ Good - organized structure
new FileId("documents", "2024/invoices/INV-001.pdf");

// ❌ Bad - flat structure
new FileId("documents", "invoice-001.pdf");
```

### 4. Handle Results Properly

```java
Result<FileObject> result = adapter.download(fileId);

// ✅ Handle both success and failure
result
    .ifSuccess(file -> processFile(file))
    .ifFailure(problem -> logError(problem));
```

### 5. Close InputStreams

```java
Result<FileObject> result = adapter.download(fileId);

result.ifSuccess(fileObject -> {
    try (InputStream content = fileObject.content()) {
        // Use content
    } catch (IOException e) {
        // Handle exception
    }
});
```

### 6. Use Appropriate Storage Tiers

```java
// Frequently accessed
UploadOptions.builder().storageClass(StorageClass.STANDARD).build();

// Infrequently accessed (>30 days)
UploadOptions.builder().storageClass(StorageClass.STANDARD_IA).build();

// Archive (rarely accessed)
UploadOptions.builder().storageClass(StorageClass.GLACIER).build();
```

## Complete Example

```java
import com.marcusprado02.commons.adapters.files.azureblob.*;
import com.marcusprado02.commons.ports.files.*;
import com.marcusprado02.commons.kernel.result.Result;

import java.io.*;
import java.util.*;

public class AzureBlobExample {
  
  public static void main(String[] args) {
    // 1. Create configuration with Managed Identity
    AzureBlobConfiguration config = AzureBlobConfiguration.withManagedIdentity(
        "https://myaccount.blob.core.windows.net",
        "myaccount"
    );
    
    // 2. Create adapter
    AzureBlobFileStoreAdapter adapter = AzureBlobClientFactory.createAdapter(config);
    
    // 3. Upload a file
    FileId fileId = new FileId("my-container", "documents/report.pdf");
    
    try (InputStream content = new FileInputStream("report.pdf")) {
      UploadOptions uploadOptions = UploadOptions.builder()
          .contentType("application/pdf")
          .metadata(Map.of(
              "author", "John Doe",
              "department", "Finance",
              "year", "2024"
          ))
          .storageClass(StorageClass.STANDARD)
          .build();
      
      Result<UploadResult> uploadResult = adapter.upload(fileId, content, uploadOptions);
      
      uploadResult.ifSuccess(result -> {
        System.out.println("✓ File uploaded successfully");
        System.out.println("  ETag: " + result.etag());
        System.out.println("  Size: " + result.size() + " bytes");
      });
    } catch (IOException e) {
      System.err.println("Failed to read file: " + e.getMessage());
      return;
    }
    
    // 4. Generate presigned URL for sharing
    Result<URL> urlResult = adapter.generatePresignedUrl(
        fileId, 
        PresignedOperation.GET, 
        Duration.ofHours(24)
    );
    
    urlResult.ifSuccess(url -> {
      System.out.println("✓ Shareable URL (valid for 24 hours):");
      System.out.println("  " + url);
    });
    
    // 5. Download the file
    Result<FileObject> downloadResult = adapter.download(fileId);
    
    downloadResult.ifSuccess(fileObject -> {
      try (InputStream stream = fileObject.content()) {
        Files.copy(stream, Path.of("downloaded-report.pdf"));
        System.out.println("✓ File downloaded successfully");
        System.out.println("  Content-Type: " + fileObject.metadata().contentType());
        System.out.println("  Size: " + fileObject.metadata().contentLength());
      } catch (IOException e) {
        System.err.println("Failed to save file: " + e.getMessage());
      }
    });
    
    // 6. List all documents
    Result<ListResult> listResult = adapter.list(
        "my-container", 
        "documents/", 
        new ListOptions(100)
    );
    
    listResult.ifSuccess(result -> {
      System.out.println("✓ Found " + result.files().size() + " documents:");
      result.files().forEach(id -> {
        System.out.println("  - " + id.key());
      });
    });
    
    // 7. Delete the file
    Result<Void> deleteResult = adapter.delete(fileId);
    
    deleteResult.ifSuccess(() -> {
      System.out.println("✓ File deleted successfully");
    });
  }
}
```

## Migration from Connection String to Managed Identity

### Before (Connection String)

```java
AzureBlobConfiguration config = AzureBlobConfiguration.withConnectionString(
    "DefaultEndpointsProtocol=https;AccountName=myaccount;AccountKey=SECRET_KEY;..."
);
```

### After (Managed Identity)

```java
// 1. Enable Managed Identity in Azure Portal for your App Service/VM/Function
// 2. Grant "Storage Blob Data Contributor" role to the Managed Identity
// 3. Update configuration

AzureBlobConfiguration config = AzureBlobConfiguration.withManagedIdentity(
    "https://myaccount.blob.core.windows.net",
    "myaccount"
);
```

## Dependencies

- **Azure Storage Blob SDK**: 12.25.1
- **Azure Identity**: 1.11.1
- **Commons Kernel Result**: For Result<T> pattern
- **Commons Kernel Errors**: For Problem/ErrorCode
- **Commons Ports Files**: FileStorePort interface

## License

See root project LICENSE file.

## Support

For issues and questions:
- Create an issue in the project repository
- Check existing documentation in `/docs`
- Review integration tests for usage examples
