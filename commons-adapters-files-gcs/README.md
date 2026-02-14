# Commons Adapters Files GCS

Google Cloud Storage (GCS) adapter for `FileStorePort`, providing type-safe file operations with multiple authentication methods and comprehensive features.

## Features

- ✅ **Upload/Download**: Stream files to/from GCS buckets
- ✅ **Signed URLs**: Generate temporary access URLs (GET/PUT)
- ✅ **Multiple Authentication**: Service Account, Application Default Credentials, Custom
- ✅ **Storage Classes**: STANDARD, NEARLINE, COLDLINE, ARCHIVE
- ✅ **Custom Metadata**: Key-value metadata support
- ✅ **Batch Operations**: Delete multiple files efficiently
- ✅ **Server-side Copy**: Copy files without downloading
- ✅ **Type-safe API**: `Result<T>` instead of exceptions
- ✅ **Testable**: fake-gcs-server support for integration tests
- ✅ **Production-ready**: Logging, error handling, best practices

## Installation

### Maven

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-files-gcs</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.marcusprado02.commons:commons-adapters-files-gcs:0.1.0-SNAPSHOT'
```

## Quick Start

### 1. Service Account Authentication (Recommended for Production)

```java
import com.marcusprado02.commons.adapters.files.gcs.*;
import com.marcusprado02.commons.ports.files.*;
import java.io.*;

// Configure with service account JSON file
var config = GCSConfiguration.withServiceAccount(
    "my-project-id",
    "/path/to/service-account.json"
);

// Create Storage client
var storage = GCSClientFactory.createStorage(config);

// Create adapter
var adapter = new GCSFileStoreAdapter(storage);

// Upload file
var fileId = new FileId("my-bucket", "documents/report.pdf");
var content = new FileInputStream("report.pdf");
var options = FileStorePort.UploadOptions.builder()
    .contentType("application/pdf")
    .metadata(Map.of("author", "john.doe", "department", "finance"))
    .storageClass(FileStorePort.StorageClass.STANDARD)
    .build();

var result = adapter.upload(fileId, content, options);

if (result.isSuccess()) {
    var uploadResult = result.getValue();
    System.out.println("Uploaded: " + uploadResult.fileId().key());
    System.out.println("ETag: " + uploadResult.etag());
    System.out.println("Size: " + uploadResult.contentLength() + " bytes");
} else {
    System.err.println("Upload failed: " + result.getProblem().detail());
}
```

### 2. Application Default Credentials (ADC)

Best for Google Cloud environments (Compute Engine, GKE, Cloud Run):

```java
// Uses credentials from:
// 1. GOOGLE_APPLICATION_CREDENTIALS environment variable
// 2. Compute Engine/GKE/Cloud Run service account
// 3. gcloud auth application-default login
var config = GCSConfiguration.withApplicationDefault("my-project-id");

var storage = GCSClientFactory.createStorage(config);
var adapter = new GCSFileStoreAdapter(storage);
```

### 3. Custom Credentials

```java
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;

Credentials customCredentials = GoogleCredentials.fromStream(
    new FileInputStream("credentials.json")
);

var config = GCSConfiguration.withCustomCredentials(
    "my-project-id",
    customCredentials
);

var storage = GCSClientFactory.createStorage(config);
var adapter = new GCSFileStoreAdapter(storage);
```

## Configuration

### Service Account Setup

1. **Create Service Account** in Google Cloud Console:
   - IAM & Admin → Service Accounts → Create Service Account
   - Grant permissions: `Storage Object Admin` or custom roles

2. **Download JSON Key**:
   - Service Account → Keys → Add Key → JSON
   - Save securely (never commit to version control)

3. **Use in Code**:
```java
var config = GCSConfiguration.withServiceAccount(
    "my-project-id",
    "/secure/path/to/service-account-key.json"
);
```

### Application Default Credentials Setup

#### Local Development

```bash
# Install gcloud CLI
# https://cloud.google.com/sdk/docs/install

# Authenticate
gcloud auth application-default login

# Set project
gcloud config set project my-project-id
```

#### Google Cloud Environments

Automatically uses attached service account:

```java
// In GKE, Cloud Run, Compute Engine - no extra config needed
var config = GCSConfiguration.withApplicationDefault("my-project-id");
```

### Builder Pattern

```java
var config = GCSConfiguration.builder()
    .projectId("my-project-id")
    .serviceAccount("/path/to/key.json")  // or .applicationDefault()
    .endpoint("http://localhost:4443")    // optional, for testing
    .build();
```

## Usage Examples

### Upload File

```java
var fileId = new FileId("my-bucket", "images/photo.jpg");
var content = new FileInputStream("photo.jpg");
var options = FileStorePort.UploadOptions.builder()
    .contentType("image/jpeg")
    .metadata(Map.of(
        "photographer", "jane.smith",
        "location", "San Francisco",
        "camera", "Canon EOS"
    ))
    .storageClass(FileStorePort.StorageClass.STANDARD)
    .build();

adapter.upload(fileId, content, options)
    .ifSuccess(result -> System.out.println("Uploaded: " + result.fileId().key()))
    .ifFailure(problem -> System.err.println("Error: " + problem.detail()));
```

### Download File

```java
var fileId = new FileId("my-bucket", "documents/invoice-2024.pdf");

adapter.download(fileId)
    .ifSuccess(fileObject -> {
        // Save to local file
        try (var output = new FileOutputStream("invoice.pdf")) {
            output.write(fileObject.content());
        }

        // Access metadata
        var metadata = fileObject.metadata();
        System.out.println("Size: " + metadata.contentLength());
        System.out.println("Type: " + metadata.contentType());
        System.out.println("Custom: " + metadata.metadata().get("author"));
    })
    .ifFailure(problem -> System.err.println("Download failed: " + problem.detail()));
```

### Check File Existence

```java
var fileId = new FileId("my-bucket", "config/settings.json");

adapter.exists(fileId)
    .ifSuccess(exists -> {
        if (exists) {
            System.out.println("File exists");
        } else {
            System.out.println("File not found");
        }
    });
```

### Get Metadata Without Downloading

```java
var fileId = new FileId("my-bucket", "videos/demo.mp4");

adapter.getMetadata(fileId)
    .ifSuccess(metadata -> {
        System.out.println("Size: " + metadata.contentLength() + " bytes");
        System.out.println("Type: " + metadata.contentType());
        System.out.println("ETag: " + metadata.etag());
        System.out.println("Created: " + metadata.createdAt());
        System.out.println("Modified: " + metadata.lastModified());
        System.out.println("Custom metadata: " + metadata.metadata());
    });
```

### List Files with Prefix

```java
var bucket = "my-bucket";
var prefix = "documents/2024/";
var options = FileStorePort.ListOptions.builder()
    .maxKeys(100)
    .build();

adapter.list(bucket, prefix, options)
    .ifSuccess(result -> {
        System.out.println("Found " + result.files().size() + " files:");
        for (var file : result.files()) {
            System.out.println("  - " + file.key());
        }

        if (result.hasMore()) {
            System.out.println("More files available. Token: " + result.continuationToken());
        }
    });
```

### List with Pagination

```java
String continuationToken = null;
var allFiles = new ArrayList<FileId>();

do {
    var options = FileStorePort.ListOptions.builder()
        .maxKeys(50)
        .continuationToken(continuationToken)
        .build();

    var result = adapter.list("my-bucket", "logs/", options);

    if (result.isSuccess()) {
        var listResult = result.getValue();
        allFiles.addAll(listResult.files());
        continuationToken = listResult.hasMore() ? listResult.continuationToken() : null;
    } else {
        break;
    }
} while (continuationToken != null);

System.out.println("Total files: " + allFiles.size());
```

### Generate Signed URL for Download (GET)

```java
var fileId = new FileId("my-bucket", "reports/annual-report-2024.pdf");
var duration = Duration.ofHours(2);

adapter.generatePresignedUrl(fileId, FileStorePort.PresignedOperation.GET, duration)
    .ifSuccess(url -> {
        System.out.println("Share this URL (valid for 2 hours):");
        System.out.println(url);

        // Send URL via email, share with client, etc.
    })
    .ifFailure(problem -> System.err.println("Failed to generate URL: " + problem.detail()));
```

### Generate Signed URL for Upload (PUT)

```java
var fileId = new FileId("my-bucket", "uploads/user-photo.jpg");
var duration = Duration.ofMinutes(30);

adapter.generatePresignedUrl(fileId, FileStorePort.PresignedOperation.PUT, duration)
    .ifSuccess(url -> {
        // Return URL to frontend for direct upload
        // Frontend can PUT file directly to this URL (no server upload needed)
        System.out.println("Upload URL (valid for 30 minutes):");
        System.out.println(url);
    });
```

### Copy File

```java
var source = new FileId("my-bucket", "originals/image.jpg");
var destination = new FileId("my-bucket", "backups/image-backup.jpg");

adapter.copy(source, destination)
    .ifSuccess(_ -> System.out.println("File copied successfully"))
    .ifFailure(problem -> System.err.println("Copy failed: " + problem.detail()));
```

### Delete Single File

```java
var fileId = new FileId("my-bucket", "temp/cache-file.tmp");

adapter.delete(fileId)
    .ifSuccess(_ -> System.out.println("File deleted"))
    .ifFailure(problem -> System.err.println("Delete failed: " + problem.detail()));
```

### Delete Multiple Files (Batch)

```java
var filesToDelete = List.of(
    new FileId("my-bucket", "temp/file1.tmp"),
    new FileId("my-bucket", "temp/file2.tmp"),
    new FileId("my-bucket", "temp/file3.tmp")
);

adapter.deleteAll(filesToDelete)
    .ifSuccess(result -> {
        System.out.println("Deleted: " + result.deletedCount() + " files");
        if (!result.failedDeletes().isEmpty()) {
            System.out.println("Failed to delete:");
            result.failedDeletes().forEach(f -> System.out.println("  - " + f.key()));
        }
    });
```

## Storage Classes

GCS offers different storage classes for cost optimization:

| FileStorePort Class | GCS Class | Use Case | Cost |
|---------------------|-----------|----------|------|
| `STANDARD` | STANDARD | Frequently accessed data | Higher storage, lower access |
| `INFREQUENT_ACCESS` | NEARLINE | Monthly access | Medium storage, medium access |
| `GLACIER` | COLDLINE | Quarterly access | Lower storage, higher access |
| `DEEP_ARCHIVE` | ARCHIVE | Annual access | Lowest storage, highest access |

**Example**:

```java
// Archive old logs to ARCHIVE storage class
var options = FileStorePort.UploadOptions.builder()
    .storageClass(FileStorePort.StorageClass.DEEP_ARCHIVE)  // Maps to GCS ARCHIVE
    .build();

adapter.upload(fileId, content, options);
```

**Retrieval Times**:
- **STANDARD/NEARLINE**: Instant access
- **COLDLINE**: Instant access
- **ARCHIVE**: Instant access (all classes in GCS have similar latency)

Note: Unlike AWS Glacier, GCS ARCHIVE provides instant access. Main difference is pricing.

## Error Handling

All operations return `Result<T>` for type-safe error handling:

```java
var result = adapter.download(fileId);

// Pattern 1: ifSuccess/ifFailure
result
    .ifSuccess(file -> processFile(file))
    .ifFailure(problem -> logError(problem));

// Pattern 2: isSuccess/isFailure
if (result.isSuccess()) {
    var file = result.getValue();
    // process file
} else {
    var problem = result.getProblem();
    System.err.println("Error: " + problem.title());
    System.err.println("Detail: " + problem.detail());
    System.err.println("Category: " + problem.category());
}

// Pattern 3: map/flatMap
var textContent = adapter.download(fileId)
    .map(file -> new String(file.content(), StandardCharsets.UTF_8))
    .getOrElse("default content");
```

### Common Error Categories

| Category | Description | Example |
|----------|-------------|---------|
| `FILE_NOT_FOUND` | File doesn't exist | Download non-existent file |
| `GCS_UPLOAD_ERROR` | Upload failed | Network error, permission denied |
| `GCS_DOWNLOAD_ERROR` | Download failed | Bucket not found, access denied |
| `GCS_DELETE_ERROR` | Delete failed | Insufficient permissions |
| `GCS_COPY_ERROR` | Copy failed | Source doesn't exist |
| `GCS_LIST_ERROR` | List failed | Bucket access denied |
| `GCS_SIGNED_URL_ERROR` | Signed URL generation failed | Invalid credentials |
| `IO_ERROR` | IO operation failed | Cannot read input stream |

## Testing

### Using fake-gcs-server with Testcontainers

```java
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MyFileStoreTest {

    @Container
    private static final GenericContainer<?> fakeGcs = new GenericContainer<>("fsouza/fake-gcs-server:latest")
        .withExposedPorts(4443)
        .withCommand("-scheme", "http");

    private GCSFileStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        var endpoint = "http://" + fakeGcs.getHost() + ":" + fakeGcs.getMappedPort(4443);

        var storage = StorageOptions.newBuilder()
            .setProjectId("test-project")
            .setHost(endpoint)
            .build()
            .getService();

        // Create test bucket
        storage.create(BucketInfo.of("test-bucket"));

        adapter = new GCSFileStoreAdapter(storage);
    }

    @Test
    void shouldUploadAndDownload() {
        var fileId = new FileId("test-bucket", "test.txt");
        var content = new ByteArrayInputStream("Hello!".getBytes());

        adapter.upload(fileId, content, UploadOptions.defaults());

        var result = adapter.download(fileId);
        assertThat(result.isSuccess()).isTrue();
    }
}
```

### Using Docker for Local Testing

```bash
# Run fake-gcs-server
docker run -d -p 4443:4443 fsouza/fake-gcs-server:latest -scheme http

# Configure adapter to use local server
var config = GCSConfiguration.forTesting("test-project", "http://localhost:4443");
var storage = GCSClientFactory.createStorage(config);
var adapter = new GCSFileStoreAdapter(storage);
```

## Performance Considerations

### Streaming for Large Files

```java
// ✅ Good: Streaming (memory-efficient)
try (var input = new FileInputStream("large-file.zip")) {
    adapter.upload(fileId, input, options);
}

// ❌ Bad: Loading entire file to memory
var allBytes = Files.readAllBytes(Path.of("large-file.zip"));
adapter.upload(fileId, new ByteArrayInputStream(allBytes), options);
```

### Batch Operations

```java
// ✅ Good: Batch delete
var files = List.of(fileId1, fileId2, fileId3, ...);
adapter.deleteAll(files);

// ❌ Bad: Individual deletes
for (var file : files) {
    adapter.delete(file);  // Multiple network calls
}
```

### Parallel Uploads

```java
var files = List.of("file1.txt", "file2.txt", "file3.txt");

files.parallelStream()
    .forEach(fileName -> {
        var fileId = new FileId("my-bucket", "uploads/" + fileName);
        try (var input = new FileInputStream(fileName)) {
            adapter.upload(fileId, input, UploadOptions.defaults());
        } catch (IOException e) {
            // handle error
        }
    });
```

## Best Practices

### ✅ Use Application Default Credentials in Google Cloud

```java
// Production on GKE/Cloud Run
var config = GCSConfiguration.withApplicationDefault("production-project");
```

### ✅ Set Appropriate Storage Classes

```java
// Frequently accessed data
.storageClass(StorageClass.STANDARD)

// Monthly backups
.storageClass(StorageClass.INFREQUENT_ACCESS)  // NEARLINE

// Annual archives
.storageClass(StorageClass.DEEP_ARCHIVE)  // ARCHIVE
```

### ✅ Use Metadata for Organization

```java
.metadata(Map.of(
    "uploaded-by", userId,
    "department", "engineering",
    "project", "project-alpha",
    "environment", "production"
))
```

### ✅ Implement Proper Error Handling

```java
adapter.upload(fileId, content, options)
    .ifFailure(problem -> {
        log.error("Upload failed: {}", problem.detail());
        metrics.increment("gcs.upload.errors", "category", problem.category());
        // Implement retry logic if needed
    });
```

### ❌ Don't Hardcode Credentials

```java
// ❌ BAD
var config = GCSConfiguration.withServiceAccount(
    "my-project",
    "/hardcoded/path/key.json"
);

// ✅ GOOD
var keyPath = System.getenv("GCS_SERVICE_ACCOUNT_PATH");
var config = GCSConfiguration.withServiceAccount("my-project", keyPath);
```

### ❌ Don't Keep Signed URLs Forever

```java
// ❌ BAD
Duration.ofDays(365)

// ✅ GOOD
Duration.ofHours(2)  // Short-lived URLs
```

## Complete Workflow Example

```java
import com.marcusprado02.commons.adapters.files.gcs.*;
import com.marcusprado02.commons.ports.files.*;
import java.io.*;
import java.time.Duration;

public class DocumentManager {

    private final GCSFileStoreAdapter adapter;

    public DocumentManager() throws IOException {
        var config = GCSConfiguration.withApplicationDefault("my-project-id");
        var storage = GCSClientFactory.createStorage(config);
        this.adapter = new GCSFileStoreAdapter(storage);
    }

    public void uploadDocument(String department, File file) {
        var fileId = new FileId(
            "company-documents",
            department + "/" + file.getName()
        );

        try (var input = new FileInputStream(file)) {
            var options = FileStorePort.UploadOptions.builder()
                .contentType(detectContentType(file))
                .metadata(Map.of(
                    "department", department,
                    "uploaded-at", Instant.now().toString()
                ))
                .storageClass(FileStorePort.StorageClass.STANDARD)
                .build();

            adapter.upload(fileId, input, options)
                .ifSuccess(result ->
                    System.out.println("Uploaded: " + result.fileId().key()))
                .ifFailure(problem ->
                    System.err.println("Upload failed: " + problem.detail()));
        } catch (IOException e) {
            System.err.println("IO error: " + e.getMessage());
        }
    }

    public String generateShareLink(String department, String fileName, Duration validity) {
        var fileId = new FileId("company-documents", department + "/" + fileName);

        return adapter.generatePresignedUrl(fileId, FileStorePort.PresignedOperation.GET, validity)
            .map(URL::toString)
            .getOrElse(null);
    }

    public void archiveOldDocuments(String department, int year) {
        var prefix = department + "/" + year + "/";

        adapter.list("company-documents", prefix)
            .ifSuccess(result -> {
                System.out.println("Found " + result.files().size() + " files to archive");

                // Move to archive storage class
                for (var fileId : result.files()) {
                    // Re-upload with ARCHIVE storage class
                    adapter.download(fileId)
                        .flatMap(file -> {
                            var archiveOptions = FileStorePort.UploadOptions.builder()
                                .contentType(file.metadata().contentType())
                                .metadata(file.metadata().metadata())
                                .storageClass(FileStorePort.StorageClass.DEEP_ARCHIVE)
                                .build();

                            return adapter.upload(
                                fileId,
                                new ByteArrayInputStream(file.content()),
                                archiveOptions
                            );
                        });
                }
            });
    }

    private String detectContentType(File file) {
        var name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }
}
```

## Dependencies

```xml
<dependency>
  <groupId>com.google.cloud</groupId>
  <artifactId>google-cloud-storage</artifactId>
  <version>2.30.0</version>
</dependency>
```

## Comparison: GCS vs S3 vs Azure Blob

| Feature | GCS | S3 | Azure Blob |
|---------|-----|-------|------------|
| **Signed URLs** | ✅ Signed URLs | ✅ Presigned URLs | ✅ SAS Tokens |
| **Auth** | Service Account, ADC | IAM Roles, Keys | Connection String, Managed Identity |
| **Storage Classes** | STANDARD, NEARLINE, COLDLINE, ARCHIVE | STANDARD, IA, GLACIER, DEEP_ARCHIVE | Hot, Cool, Archive |
| **Multipart Upload** | Automatic (SDK handles) | Manual configuration | Automatic (SDK handles) |
| **Instant Retrieval** | All classes | STANDARD, IA (Glacier: hours/days) | Hot, Cool (Archive: hours) |
| **Testing** | fake-gcs-server | LocalStack | Azurite |
| **Emulator** | ✅ Official emulator | ❌ Third-party only | ✅ Official Azurite |

## When to Use GCS

✅ **Use GCS when**:
- Running on Google Cloud Platform (GKE, Cloud Run, Compute Engine)
- Need instant access across all storage classes
- Want simple authentication with Application Default Credentials
- Prefer Google's ecosystem and pricing model
- Need strong consistency (all storage classes)
- Want automatic encryption at rest

❌ **Consider alternatives when**:
- Already on AWS (use S3) or Azure (use Azure Blob)
- Need AWS-specific features (Glacier Deep Archive pricing, S3 Select)
- Want Azure-specific integrations (Azure AD, Azure Functions)

## License

This project is licensed under the MIT License.
