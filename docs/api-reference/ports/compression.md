# Port: Compression

## Visão Geral

`commons-ports-compression` define contratos para compressão/descompressão de dados (Gzip, Zip, Tar), útil para reduzir tamanho de payloads e arquivos.

**Quando usar:**
- Compressão de responses HTTP
- Arquivamento de logs
- Backup de dados
- File uploads/downloads
- Redução de storage costs

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-compression</artifactId>
    <version>${commons.version}</version>
</dependency>

<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-compression</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🗜️ Compressor Interface

```java
public interface Compressor {
    
    /**
     * Comprime dados.
     */
    Result<byte[]> compress(byte[] data);
    
    /**
     * Descomprime dados.
     */
    Result<byte[]> decompress(byte[] compressedData);
    
    /**
     * Comprime arquivo.
     */
    Result<byte[]> compressFile(String filePath);
    
    /**
     * Cria arquivo comprimido com múltiplos arquivos.
     */
    Result<byte[]> compressMultiple(Map<String, byte[]> files);
}
```

---

## 💡 Usage Examples

### HTTP Response Compression

```java
@Service
public class ReportService {
    
    private final Compressor gzipCompressor;
    
    public Result<byte[]> generateCompressedReport() {
        byte[] reportData = generateLargeReport();
        
        return gzipCompressor.compress(reportData)
            .andThen(compressed -> {
                log.info("Report compressed")
                    .field("original", reportData.length)
                    .field("compressed", compressed.length)
                    .field("ratio", (double) compressed.length / reportData.length)
                    .log();
            });
    }
}
```

### Log Archiving

```java
@Service
public class LogArchiveService {
    
    private final Compressor zipCompressor;
    private final FileStorage fileStorage;
    
    @Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
    public void archiveLogs() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        Map<String, byte[]> logFiles = collectLogFiles(yesterday);
        
        Result<byte[]> zipResult = zipCompressor.compressMultiple(logFiles);
        
        if (zipResult.isError()) {
            log.error("Failed to compress logs").error(zipResult.getError()).log();
            return;
        }
        
        String key = String.format(
            "logs/archive/%d/%02d/%02d.zip",
            yesterday.getYear(),
            yesterday.getMonthValue(),
            yesterday.getDayOfMonth()
        );
        
        fileStorage.store(key, zipResult.get());
    }
}
```

---

## Ver Também

- [Compression Adapter](../../../commons-adapters-compression/) - Gzip/Zip implementation
- [Files](./files.md) - File storage
