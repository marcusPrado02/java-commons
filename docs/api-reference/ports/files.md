# Port: File Storage

## Visão Geral

`commons-ports-files` define contratos para armazenamento de arquivos, abstraindo implementações como S3, Azure Blob Storage, Google Cloud Storage, ou filesystem local.

**Quando usar:**
- Upload de imagens e documentos
- Armazenamento de backups
- Content delivery (CDN)
- Data lakes e archives
- User-generated content

**Implementações disponíveis:**
- `commons-adapters-files-s3` - Amazon S3
- `commons-adapters-files-azure-blob` - Azure Blob Storage
- `commons-adapters-files-gcs` - Google Cloud Storage

---

## 📦 Instalação

```xml
<!-- Port (interface) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-files</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Adapter (implementação) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-files-s3</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Interfaces

### FileStorage

Interface principal para file storage.

```java
public interface FileStorage {
    
    /**
     * Armazena arquivo.
     */
    Result<FileLocation> store(String key, byte[] content);
    
    /**
     * Armazena arquivo com metadados.
     */
    Result<FileLocation> store(String key, byte[] content, FileMetadata metadata);
    
    /**
     * Armazena de InputStream.
     */
    Result<FileLocation> store(String key, InputStream content, long contentLength);
    
    /**
     * Recupera arquivo.
     */
    Result<byte[]> retrieve(String key);
    
    /**
     * Recupera como InputStream.
     */
    Result<InputStream> retrieveAsStream(String key);
    
    /**
     * Deleta arquivo.
     */
    Result<Void> delete(String key);
    
    /**
     * Verifica se arquivo existe.
     */
    boolean exists(String key);
    
    /**
     * Gera URL pré-assinada.
     */
    Result<String> generatePresignedUrl(String key, Duration expiration);
    
    /**
     * Lista arquivos por prefixo.
     */
    Result<List<FileInfo>> list(String prefix);
}
```

### FileMetadata

Metadados do arquivo.

```java
public record FileMetadata(
    String contentType,
    long contentLength,
    Map<String, String> customMetadata,
    Optional<String> cacheControl,
    Optional<String> contentDisposition
) {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        public Builder contentType(String contentType);
        public Builder contentLength(long contentLength);
        public Builder metadata(String key, String value);
        public Builder cacheControl(String cacheControl);
        public Builder contentDisposition(String disposition);
        public FileMetadata build();
    }
}
```

### FileLocation

Referência ao arquivo armazenado.

```java
public record FileLocation(
    String key,
    String url,
    String etag,
    Instant uploadedAt
) {}
```

---

## 💡 Basic Usage

### Image Upload

```java
@Service
public class ImageService {
    
    private final FileStorage fileStorage;
    
    public Result<ImageId> uploadImage(
        UserId userId,
        byte[] imageData,
        String filename
    ) {
        // Generate unique key
        ImageId imageId = ImageId.generate();
        String key = String.format(
            "images/%s/%s/%s",
            userId.value(),
            imageId.value(),
            filename
        );
        
        // Detect content type
        String contentType = detectContentType(filename);
        
        // Create metadata
        FileMetadata metadata = FileMetadata.builder()
            .contentType(contentType)
            .contentLength(imageData.length)
            .metadata("userId", userId.value())
            .metadata("uploadDate", Instant.now().toString())
            .cacheControl("public, max-age=31536000") // 1 year
            .build();
        
        // Store in cloud storage
        return fileStorage.store(key, imageData, metadata)
            .map(location -> imageId);
    }
    
    public Result<byte[]> downloadImage(ImageId imageId, UserId userId) {
        String key = String.format(
            "images/%s/%s/*",
            userId.value(),
            imageId.value()
        );
        
        // Find image
        return fileStorage.list(key)
            .flatMap(files -> {
                if (files.isEmpty()) {
                    return Result.fail(Problem.of(
                        "IMAGE.NOT_FOUND",
                        "Image not found: " + imageId
                    ));
                }
                
                FileInfo file = files.get(0);
                return fileStorage.retrieve(file.key());
            });
    }
}
```

### Document Storage

```java
@Service
public class DocumentService {
    
    private final FileStorage fileStorage;
    
    public Result<DocumentId> storeDocument(
        MultipartFile file,
        UserId userId
    ) {
        try {
            DocumentId documentId = DocumentId.generate();
            
            String key = String.format(
                "documents/%s/%s",
                userId.value(),
                documentId.value()
            );
            
            FileMetadata metadata = FileMetadata.builder()
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .metadata("filename", file.getOriginalFilename())
                .metadata("uploadedBy", userId.value())
                .contentDisposition("attachment; filename=\"" + 
                    file.getOriginalFilename() + "\"")
                .build();
            
            return fileStorage.store(
                key,
                file.getInputStream(),
                file.getSize()
            ).map(location -> documentId);
            
        } catch (IOException e) {
            return Result.fail(Problem.of(
                "DOCUMENT.UPLOAD_FAILED",
                e.getMessage()
            ));
        }
    }
    
    public Result<String> getDownloadUrl(
        DocumentId documentId,
        UserId userId,
        Duration expiration
    ) {
        String key = String.format(
            "documents/%s/%s",
            userId.value(),
            documentId.value()
        );
        
        return fileStorage.generatePresignedUrl(key, expiration);
    }
}
```

---

## 🖼️ Image Processing

### Thumbnail Generation

```java
@Service
public class ThumbnailService {
    
    private final FileStorage fileStorage;
    
    public Result<String> generateThumbnail(
        String originalKey,
        int width,
        int height
    ) {
        // Download original
        return fileStorage.retrieve(originalKey)
            .flatMap(originalBytes -> {
                // Resize image
                byte[] thumbnail = resize(originalBytes, width, height);
                
                // Generate thumbnail key
                String thumbnailKey = originalKey
                    .replace("/images/", "/thumbnails/")
                    + String.format("_%dx%d", width, height);
                
                // Store thumbnail
                FileMetadata metadata = FileMetadata.builder()
                    .contentType("image/jpeg")
                    .contentLength(thumbnail.length)
                    .metadata("originalKey", originalKey)
                    .cacheControl("public, max-age=31536000")
                    .build();
                
                return fileStorage.store(thumbnailKey, thumbnail, metadata)
                    .map(FileLocation::url);
            });
    }
    
    private byte[] resize(byte[] imageBytes, int width, int height) {
        try {
            BufferedImage original = ImageIO.read(
                new ByteArrayInputStream(imageBytes)
            );
            
            BufferedImage resized = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
            );
            
            Graphics2D g = resized.createGraphics();
            g.drawImage(
                original.getScaledInstance(width, height, Image.SCALE_SMOOTH),
                0, 0,
                null
            );
            g.dispose();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", baos);
            
            return baos.toByteArray();
            
        } catch (IOException e) {
            throw new RuntimeException("Image resize failed", e);
        }
    }
}
```

---

## 📊 Large File Upload

### Multipart Upload

```java
public interface MultipartFileStorage extends FileStorage {
    
    /**
     * Inicia upload multipart.
     */
    Result<String> initiateMultipartUpload(String key);
    
    /**
     * Upload de parte.
     */
    Result<String> uploadPart(
        String key,
        String uploadId,
        int partNumber,
        byte[] data
    );
    
    /**
     * Completa upload multipart.
     */
    Result<FileLocation> completeMultipartUpload(
        String key,
        String uploadId,
        List<String> partETags
    );
    
    /**
     * Aborta upload multipart.
     */
    Result<Void> abortMultipartUpload(String key, String uploadId);
}
```

### Chunked Upload Service

```java
@Service
public class LargeFileUploadService {
    
    private final MultipartFileStorage fileStorage;
    private static final int CHUNK_SIZE = 5 * 1024 * 1024; // 5 MB
    
    public Result<FileLocation> uploadLargeFile(
        String key,
        InputStream inputStream,
        long totalSize
    ) {
        // Initiate multipart upload
        Result<String> uploadIdResult = fileStorage.initiateMultipartUpload(key);
        
        if (uploadIdResult.isFail()) {
            return Result.fail(uploadIdResult.problemOrNull());
        }
        
        String uploadId = uploadIdResult.getOrThrow();
        
        try {
            List<String> partETags = new ArrayList<>();
            int partNumber = 1;
            
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // Upload part
                byte[] partData = Arrays.copyOf(buffer, bytesRead);
                
                Result<String> partResult = fileStorage.uploadPart(
                    key,
                    uploadId,
                    partNumber,
                    partData
                );
                
                if (partResult.isFail()) {
                    fileStorage.abortMultipartUpload(key, uploadId);
                    return Result.fail(partResult.problemOrNull());
                }
                
                partETags.add(partResult.getOrThrow());
                partNumber++;
            }
            
            // Complete upload
            return fileStorage.completeMultipartUpload(key, uploadId, partETags);
            
        } catch (IOException e) {
            fileStorage.abortMultipartUpload(key, uploadId);
            return Result.fail(Problem.of(
                "FILE.UPLOAD_FAILED",
                e.getMessage()
            ));
        }
    }
}
```

---

## 🔐 Secure File Access

### Presigned URLs

```java
@RestController
@RequestMapping("/api/files")
public class FileController {
    
    private final FileStorage fileStorage;
    private final FileAccessService fileAccessService;
    
    @GetMapping("/{fileId}/download")
    public ResponseEntity<?> downloadFile(
        @PathVariable FileId fileId,
        @AuthenticationPrincipal User user
    ) {
        // Check access permission
        Result<Void> accessCheck = fileAccessService.checkAccess(fileId, user.id());
        
        if (accessCheck.isFail()) {
            return ResponseEntity.status(403).build();
        }
        
        // Generate presigned URL (valid for 1 hour)
        String key = "files/" + fileId.value();
        
        return fileStorage.generatePresignedUrl(key, Duration.ofHours(1))
            .map(url -> ResponseEntity
                .status(302) // Redirect
                .header("Location", url)
                .build()
            )
            .getOrElse(problem -> 
                ResponseEntity.status(500).body(problem)
            );
    }
}
```

---

## 📁 File Organization

### Directory Structure

```java
@Service
public class FileOrganizationService {
    
    private final FileStorage fileStorage;
    
    /**
     * Organiza arquivos por tenant e tipo.
     */
    public String buildKey(
        TenantId tenantId,
        FileType fileType,
        UserId userId,
        String filename
    ) {
        LocalDate date = LocalDate.now();
        
        return String.format(
            "%s/%s/%s/%d/%02d/%02d/%s/%s",
            tenantId.value(),
            fileType.name().toLowerCase(),
            userId.value(),
            date.getYear(),
            date.getMonthValue(),
            date.getDayOfMonth(),
            UUID.randomUUID(),
            sanitizeFilename(filename)
        );
    }
    
    /**
     * Lista arquivos de usuário.
     */
    public Result<List<FileInfo>> listUserFiles(
        TenantId tenantId,
        FileType fileType,
        UserId userId
    ) {
        String prefix = String.format(
            "%s/%s/%s/",
            tenantId.value(),
            fileType.name().toLowerCase(),
            userId.value()
        );
        
        return fileStorage.list(prefix);
    }
    
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

public enum FileType {
    IMAGE,
    DOCUMENT,
    VIDEO,
    BACKUP,
    EXPORT
}
```

---

## 🔄 File Lifecycle

### Auto-Delete Old Files

```java
@Component
public class FileCleanupJob implements ScheduledTask {
    
    private final FileStorage fileStorage;
    private final Duration retentionPeriod = Duration.ofDays(90);
    
    @Override
    public String name() {
        return "cleanup-old-files";
    }
    
    @Override
    public String cronExpression() {
        return "0 0 3 * * ?"; // 3 AM daily
    }
    
    @Override
    public Result<Void> execute() {
        // List all files in temp directory
        return fileStorage.list("temp/")
            .flatMap(files -> {
                Instant cutoff = Instant.now().minus(retentionPeriod);
                
                List<String> toDelete = files.stream()
                    .filter(file -> file.lastModified().isBefore(cutoff))
                    .map(FileInfo::key)
                    .toList();
                
                log.info("Deleting old files")
                    .field("count", toDelete.size())
                    .log();
                
                // Delete in batches
                for (String key : toDelete) {
                    fileStorage.delete(key);
                }
                
                return Result.ok();
            });
    }
}
```

---

## 🧪 Testing

### Mock File Storage

```java
public class InMemoryFileStorage implements FileStorage {
    
    private final Map<String, StoredFile> files = new ConcurrentHashMap<>();
    
    @Override
    public Result<FileLocation> store(String key, byte[] content) {
        StoredFile file = new StoredFile(
            key,
            content,
            Instant.now(),
            UUID.randomUUID().toString()
        );
        
        files.put(key, file);
        
        FileLocation location = new FileLocation(
            key,
            "http://localhost:8080/files/" + key,
            file.etag(),
            file.uploadedAt()
        );
        
        return Result.ok(location);
    }
    
    @Override
    public Result<byte[]> retrieve(String key) {
        StoredFile file = files.get(key);
        
        if (file == null) {
            return Result.fail(Problem.of(
                "FILE.NOT_FOUND",
                "File not found: " + key
            ));
        }
        
        return Result.ok(file.content());
    }
    
    @Override
    public Result<Void> delete(String key) {
        files.remove(key);
        return Result.ok();
    }
    
    @Override
    public boolean exists(String key) {
        return files.containsKey(key);
    }
    
    private record StoredFile(
        String key,
        byte[] content,
        Instant uploadedAt,
        String etag
    ) {}
}
```

### Test Example

```java
class ImageServiceTest {
    
    private FileStorage fileStorage;
    private ImageService imageService;
    
    @BeforeEach
    void setUp() {
        fileStorage = new InMemoryFileStorage();
        imageService = new ImageService(fileStorage);
    }
    
    @Test
    void shouldUploadImage() {
        // Given
        UserId userId = UserId.generate();
        byte[] imageData = loadTestImage();
        
        // When
        Result<ImageId> result = imageService.uploadImage(
            userId,
            imageData,
            "profile.jpg"
        );
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        ImageId imageId = result.getOrThrow();
        assertThat(imageId).isNotNull();
        
        // Verify file stored
        String expectedKey = String.format(
            "images/%s/%s/profile.jpg",
            userId.value(),
            imageId.value()
        );
        
        assertThat(fileStorage.exists(expectedKey)).isTrue();
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use structured keys
String key = String.format("images/%s/%s", userId, imageId);

// ✅ Set content type
FileMetadata metadata = FileMetadata.builder()
    .contentType("image/jpeg")
    .build();

// ✅ Use presigned URLs para security
String url = fileStorage.generatePresignedUrl(key, Duration.ofHours(1));

// ✅ Organize com prefixes
fileStorage.list("images/" + userId);

// ✅ Cleanup old files
@Scheduled(cron = "0 0 3 * * ?")
public void cleanupOldFiles() { }
```

### ❌ DON'T

```java
// ❌ NÃO armazene paths absolutos
String key = "/var/www/images/photo.jpg";  // ❌ Use keys relativos!

// ❌ NÃO exponha URLs diretas sem controle
return "https://bucket.s3.amazonaws.com/file.jpg";  // ❌ Use presigned!

// ❌ NÃO ignore content type
fileStorage.store(key, data);  // ❌ Sem metadata!

// ❌ NÃO deixe arquivos temporários sem cleanup
fileStorage.store("temp/" + uuid);  // ❌ Vai acumular!

// ❌ NÃO carregue arquivos grandes na memória
byte[] huge = fileStorage.retrieve(key);  // ❌ OutOfMemoryError!
// ✅ Use streams
```

---

## Ver Também

- [S3 Adapter](../../../commons-adapters-files-s3/) - Amazon S3 implementation
- [Azure Blob Adapter](../../../commons-adapters-files-azure-blob/) - Azure implementation
- [GCS Adapter](../../../commons-adapters-files-gcs/) - Google Cloud Storage
- [Backup & Restore](../app-backup-restore.md) - File backups
