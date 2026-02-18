# Cloud Storage Adapter Guide (S3 / Azure Blob)

## Overview

This guide covers **cloud storage adapters** for AWS S3 and Azure Blob Storage.

**Key Features:**
- File upload/download
- Multipart upload (large files)
- Presigned URLs
- Lifecycle policies
- Versioning
- Encryption
- Metadata management

---

## 📦 Installation

### AWS S3

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-files-s3</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- AWS SDK v2 -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
</dependency>
```

### Azure Blob Storage

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-files-azure-blob</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Azure SDK -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-storage-blob</artifactId>
</dependency>
```

---

## ⚙️ Configuration

### AWS S3 Configuration

```yaml
# application.yml
cloud:
  aws:
    credentials:
      access-key: ${AWS_ACCESS_KEY_ID}
      secret-key: ${AWS_SECRET_ACCESS_KEY}
    region:
      static: us-east-1
    s3:
      bucket: my-app-bucket
      endpoint: https://s3.us-east-1.amazonaws.com
```

```java
@Configuration
public class S3Config {
    
    @Value("${cloud.aws.region.static}")
    private String region;
    
    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;
    
    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;
    
    @Bean
    public S3Client s3Client() {
        AwsCredentials credentials = AwsBasicCredentials.create(
            accessKey,
            secretKey
        );
        
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
    }
    
    @Bean
    public S3TransferManager transferManager(S3Client s3Client) {
        return S3TransferManager.builder()
            .s3Client(s3Client)
            .build();
    }
}
```

### Azure Blob Configuration

```yaml
# application.yml
azure:
  storage:
    blob:
      connection-string: ${AZURE_STORAGE_CONNECTION_STRING}
      container-name: my-app-container
```

```java
@Configuration
public class AzureBlobConfig {
    
    @Value("${azure.storage.blob.connection-string}")
    private String connectionString;
    
    @Value("${azure.storage.blob.container-name}")
    private String containerName;
    
    @Bean
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
    }
    
    @Bean
    public BlobContainerClient blobContainerClient(
        BlobServiceClient blobServiceClient
    ) {
        BlobContainerClient containerClient = 
            blobServiceClient.getBlobContainerClient(containerName);
        
        // Create container if not exists
        if (!containerClient.exists()) {
            containerClient.create();
        }
        
        return containerClient;
    }
}
```

---

## 📤 File Upload

### AWS S3 Upload

```java
@Service
public class S3FileStorageService implements FileStorageService {
    
    private final S3Client s3Client;
    private final String bucketName;
    
    @Override
    public Result<FileMetadata> upload(
        String fileName,
        InputStream inputStream,
        long contentLength,
        String contentType
    ) {
        try {
            String key = generateKey(fileName);
            
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .metadata(Map.of(
                    "original-name", fileName,
                    "uploaded-at", Instant.now().toString()
                ))
                .build();
            
            PutObjectResponse response = s3Client.putObject(
                request,
                RequestBody.fromInputStream(inputStream, contentLength)
            );
            
            log.info("File uploaded to S3")
                .field("key", key)
                .field("etag", response.eTag())
                .log();
            
            FileMetadata metadata = new FileMetadata(
                key,
                fileName,
                contentLength,
                contentType,
                LocalDateTime.now()
            );
            
            return Result.ok(metadata);
            
        } catch (Exception e) {
            log.error("Failed to upload file to S3")
                .exception(e)
                .field("fileName", fileName)
                .log();
            
            return Result.error(Error.of("UPLOAD_ERROR", e.getMessage()));
        }
    }
    
    private String generateKey(String fileName) {
        String uuid = UUID.randomUUID().toString();
        String extension = fileName.substring(fileName.lastIndexOf("."));
        return "uploads/" + LocalDate.now() + "/" + uuid + extension;
    }
}
```

### Azure Blob Upload

```java
@Service
public class AzureBlobStorageService implements FileStorageService {
    
    private final BlobContainerClient containerClient;
    
    @Override
    public Result<FileMetadata> upload(
        String fileName,
        InputStream inputStream,
        long contentLength,
        String contentType
    ) {
        try {
            String blobName = generateBlobName(fileName);
            
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            Map<String, String> metadata = Map.of(
                "original-name", fileName,
                "uploaded-at", Instant.now().toString()
            );
            
            BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(contentType);
            
            blobClient.upload(
                inputStream,
                contentLength,
                true  // Overwrite if exists
            );
            
            blobClient.setMetadata(metadata);
            blobClient.setHttpHeaders(headers);
            
            log.info("File uploaded to Azure Blob")
                .field("blobName", blobName)
                .log();
            
            FileMetadata fileMetadata = new FileMetadata(
                blobName,
                fileName,
                contentLength,
                contentType,
                LocalDateTime.now()
            );
            
            return Result.ok(fileMetadata);
            
        } catch (Exception e) {
            log.error("Failed to upload file to Azure Blob")
                .exception(e)
                .field("fileName", fileName)
                .log();
            
            return Result.error(Error.of("UPLOAD_ERROR", e.getMessage()));
        }
    }
}
```

---

## 📥 File Download

### AWS S3 Download

```java
@Service
public class S3FileStorageService {
    
    @Override
    public Result<InputStream> download(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            
            ResponseInputStream<GetObjectResponse> response = 
                s3Client.getObject(request);
            
            log.info("File downloaded from S3")
                .field("key", key)
                .log();
            
            return Result.ok(response);
            
        } catch (NoSuchKeyException e) {
            return Result.error(Error.of("FILE_NOT_FOUND", "File not found: " + key));
            
        } catch (Exception e) {
            log.error("Failed to download file from S3")
                .exception(e)
                .log();
            
            return Result.error(Error.of("DOWNLOAD_ERROR", e.getMessage()));
        }
    }
    
    @Override
    public Result<byte[]> downloadBytes(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            
            ResponseBytes<GetObjectResponse> response = 
                s3Client.getObjectAsBytes(request);
            
            return Result.ok(response.asByteArray());
            
        } catch (Exception e) {
            return Result.error(Error.of("DOWNLOAD_ERROR", e.getMessage()));
        }
    }
}
```

### Azure Blob Download

```java
@Service
public class AzureBlobStorageService {
    
    @Override
    public Result<InputStream> download(String blobName) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            if (!blobClient.exists()) {
                return Result.error(Error.of(
                    "FILE_NOT_FOUND",
                    "Blob not found: " + blobName
                ));
            }
            
            InputStream inputStream = blobClient.openInputStream();
            
            log.info("File downloaded from Azure Blob")
                .field("blobName", blobName)
                .log();
            
            return Result.ok(inputStream);
            
        } catch (Exception e) {
            log.error("Failed to download file from Azure Blob")
                .exception(e)
                .log();
            
            return Result.error(Error.of("DOWNLOAD_ERROR", e.getMessage()));
        }
    }
    
    @Override
    public Result<byte[]> downloadBytes(String blobName) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.downloadStream(outputStream);
            
            return Result.ok(outputStream.toByteArray());
            
        } catch (Exception e) {
            return Result.error(Error.of("DOWNLOAD_ERROR", e.getMessage()));
        }
    }
}
```

---

## 🔗 Presigned URLs

### AWS S3 Presigned URLs

```java
@Service
public class S3PresignedUrlService {
    
    private final S3Presigner presigner;
    
    public S3PresignedUrlService() {
        this.presigner = S3Presigner.create();
    }
    
    public Result<String> generateUploadUrl(
        String key,
        Duration expiration
    ) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            
            PutObjectPresignRequest presignRequest = 
                PutObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .putObjectRequest(request)
                    .build();
            
            PresignedPutObjectRequest presigned = 
                presigner.presignPutObject(presignRequest);
            
            String url = presigned.url().toString();
            
            log.info("Generated S3 upload URL")
                .field("key", key)
                .field("expiresIn", expiration.toMinutes() + " minutes")
                .log();
            
            return Result.ok(url);
            
        } catch (Exception e) {
            return Result.error(Error.of("PRESIGN_ERROR", e.getMessage()));
        }
    }
    
    public Result<String> generateDownloadUrl(
        String key,
        Duration expiration
    ) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            
            GetObjectPresignRequest presignRequest = 
                GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(request)
                    .build();
            
            PresignedGetObjectRequest presigned = 
                presigner.presignGetObject(presignRequest);
            
            return Result.ok(presigned.url().toString());
            
        } catch (Exception e) {
            return Result.error(Error.of("PRESIGN_ERROR", e.getMessage()));
        }
    }
}
```

### Azure Blob SAS URLs

```java
@Service
public class AzureBlobSasService {
    
    private final BlobContainerClient containerClient;
    
    public Result<String> generateUploadUrl(
        String blobName,
        Duration expiration
    ) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            OffsetDateTime expiryTime = OffsetDateTime.now().plus(expiration);
            
            BlobSasPermission permission = new BlobSasPermission()
                .setWritePermission(true)
                .setCreatePermission(true);
            
            BlobServiceSasSignatureValues sasValues = 
                new BlobServiceSasSignatureValues(expiryTime, permission);
            
            String sasToken = blobClient.generateSas(sasValues);
            String url = blobClient.getBlobUrl() + "?" + sasToken;
            
            log.info("Generated Azure Blob upload URL")
                .field("blobName", blobName)
                .log();
            
            return Result.ok(url);
            
        } catch (Exception e) {
            return Result.error(Error.of("SAS_ERROR", e.getMessage()));
        }
    }
    
    public Result<String> generateDownloadUrl(
        String blobName,
        Duration expiration
    ) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            OffsetDateTime expiryTime = OffsetDateTime.now().plus(expiration);
            
            BlobSasPermission permission = new BlobSasPermission()
                .setReadPermission(true);
            
            BlobServiceSasSignatureValues sasValues = 
                new BlobServiceSasSignatureValues(expiryTime, permission);
            
            String sasToken = blobClient.generateSas(sasValues);
            String url = blobClient.getBlobUrl() + "?" + sasToken;
            
            return Result.ok(url);
            
        } catch (Exception e) {
            return Result.error(Error.of("SAS_ERROR", e.getMessage()));
        }
    }
}
```

---

## 📂 Multipart Upload (Large Files)

### AWS S3 Multipart Upload

```java
@Service
public class S3MultipartUploadService {
    
    private final S3TransferManager transferManager;
    
    public Result<FileMetadata> uploadLargeFile(
        String fileName,
        Path filePath
    ) {
        try {
            String key = generateKey(fileName);
            
            UploadFileRequest uploadRequest = UploadFileRequest.builder()
                .putObjectRequest(req -> req
                    .bucket(bucketName)
                    .key(key)
                )
                .source(filePath)
                .build();
            
            FileUpload upload = transferManager.uploadFile(uploadRequest);
            
            CompletedFileUpload result = upload.completionFuture().join();
            
            log.info("Large file uploaded to S3")
                .field("key", key)
                .field("size", Files.size(filePath))
                .log();
            
            return Result.ok(new FileMetadata(
                key,
                fileName,
                Files.size(filePath),
                Files.probeContentType(filePath),
                LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("Failed to upload large file")
                .exception(e)
                .log();
            
            return Result.error(Error.of("UPLOAD_ERROR", e.getMessage()));
        }
    }
}
```

### Azure Blob Block Upload

```java
@Service
public class AzureBlobBlockUploadService {
    
    private final BlobContainerClient containerClient;
    
    public Result<FileMetadata> uploadLargeFile(
        String fileName,
        Path filePath
    ) {
        try {
            String blobName = generateBlobName(fileName);
            
            BlockBlobClient blockBlobClient = 
                containerClient.getBlobClient(blobName).getBlockBlobClient();
            
            // Upload in blocks
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                blockBlobClient.upload(
                    inputStream,
                    Files.size(filePath),
                    true
                );
            }
            
            log.info("Large file uploaded to Azure Blob")
                .field("blobName", blobName)
                .field("size", Files.size(filePath))
                .log();
            
            return Result.ok(new FileMetadata(
                blobName,
                fileName,
                Files.size(filePath),
                Files.probeContentType(filePath),
                LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("Failed to upload large file")
                .exception(e)
                .log();
            
            return Result.error(Error.of("UPLOAD_ERROR", e.getMessage()));
        }
    }
}
```

---

## 🗑️ File Deletion

### AWS S3 Delete

```java
@Service
public class S3FileStorageService {
    
    @Override
    public Result<Void> delete(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            
            s3Client.deleteObject(request);
            
            log.info("File deleted from S3")
                .field("key", key)
                .log();
            
            return Result.ok();
            
        } catch (Exception e) {
            log.error("Failed to delete file from S3")
                .exception(e)
                .log();
            
            return Result.error(Error.of("DELETE_ERROR", e.getMessage()));
        }
    }
    
    public Result<Void> deleteBatch(List<String> keys) {
        try {
            List<ObjectIdentifier> objectIds = keys.stream()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();
            
            Delete delete = Delete.builder()
                .objects(objectIds)
                .build();
            
            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                .bucket(bucketName)
                .delete(delete)
                .build();
            
            s3Client.deleteObjects(request);
            
            log.info("Batch deleted files from S3")
                .field("count", keys.size())
                .log();
            
            return Result.ok();
            
        } catch (Exception e) {
            return Result.error(Error.of("DELETE_ERROR", e.getMessage()));
        }
    }
}
```

### Azure Blob Delete

```java
@Service
public class AzureBlobStorageService {
    
    @Override
    public Result<Void> delete(String blobName) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.delete();
            
            log.info("Blob deleted")
                .field("blobName", blobName)
                .log();
            
            return Result.ok();
            
        } catch (Exception e) {
            log.error("Failed to delete blob")
                .exception(e)
                .log();
            
            return Result.error(Error.of("DELETE_ERROR", e.getMessage()));
        }
    }
}
```

---

## 🧪 Testing

### S3 Test with LocalStack

```java
@SpringBootTest
@Testcontainers
class S3FileStorageServiceTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:latest")
    ).withServices(LocalStackContainer.Service.S3);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("cloud.aws.s3.endpoint", 
            () -> localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("cloud.aws.credentials.access-key", () -> "test");
        registry.add("cloud.aws.credentials.secret-key", () -> "test");
        registry.add("cloud.aws.region.static", () -> "us-east-1");
    }
    
    @Autowired
    private S3FileStorageService storageService;
    
    @Test
    void shouldUploadFile() throws Exception {
        // Given
        String fileName = "test.txt";
        byte[] content = "Hello World".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);
        
        // When
        Result<FileMetadata> result = storageService.upload(
            fileName,
            inputStream,
            content.length,
            "text/plain"
        );
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get().originalName()).isEqualTo(fileName);
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use presigned URLs para client upload
String uploadUrl = generateUploadUrl(key, Duration.ofMinutes(15));

// ✅ Set metadata
.metadata(Map.of("uploaded-by", userId))

// ✅ Use multipart para large files
transferManager.uploadFile(request)

// ✅ Set lifecycle policies
// Delete old files after 90 days

// ✅ Enable versioning
// Protect against accidental deletion
```

### ❌ DON'T

```java
// ❌ NÃO store large files in memory
byte[] file = downloadBytes(key);  // ❌ OOM!

// ❌ NÃO expose storage URLs directly
return "s3://bucket/file";  // ❌ Use presigned URLs!

// ❌ NÃO ignore encryption
// Enable server-side encryption

// ❌ NÃO use blocking I/O
// Use streams for large files

// ❌ NÃO store credentials in code
// Use IAM roles or managed identities
```

---

## Ver Também

- [Files Port](../api-reference/ports/files.md) - Port interface
- [File Handling](../guides/file-handling.md) - File patterns
- [Cloud Security](../guides/cloud-security.md) - Security best practices
