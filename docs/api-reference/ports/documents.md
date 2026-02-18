# Port: Documents

## Visão Geral

`commons-ports-documents` define contratos para geração de documentos (PDF, Excel, CSV), abstraindo bibliotecas como iText e Apache POI.

**Quando usar:**
- Geração de PDFs
- Exportação Excel/CSV
- Relatórios dinâmicos
- Invoices e documentos legais
- Data exports

**Implementações disponíveis:**
- `commons-adapters-pdf-itext` - PDF generation com iText
- `commons-adapters-excel-poi` - Excel generation com Apache POI

---

## 📦 Instalação

```xml
<!-- Port (interface) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-documents</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Adapters -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-pdf-itext</artifactId>
    <version>${commons.version}</version>
</dependency>

<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-excel-poi</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 📄 DocumentGenerator Interface

### Core Methods

```java
public interface DocumentGenerator {
    
    /**
     * Gera documento a partir de HTML.
     */
    Result<byte[]> fromHtml(String html);
    
    /**
     * Gera documento a partir de template.
     */
    Result<byte[]> fromTemplate(
        String templateName,
        Map<String, Object> variables
    );
    
    /**
     * Gera documento com metadata.
     */
    Result<byte[]> generate(DocumentRequest request);
}
```

### Document Request

```java
public record DocumentRequest(
    String content,
    DocumentFormat format,
    DocumentMetadata metadata,
    Map<String, Object> options
) {
    public static Builder builder() {
        return new Builder();
    }
}

public enum DocumentFormat {
    PDF,
    EXCEL,
    CSV,
    WORD
}

public record DocumentMetadata(
    String title,
    String author,
    Optional<String> subject,
    Optional<String> keywords,
    LocalDateTime createdAt
) {}
```

---

## 📄 PDF Generation

### Invoice PDF Service

```java
@Service
public class InvoicePdfService {
    
    private final DocumentGenerator pdfGenerator;
    private final TemplateEngine templateEngine;
    private final FileStorage fileStorage;
    
    public Result<FileLocation> generateInvoicePdf(Invoice invoice) {
        // Render HTML from template
        Map<String, Object> variables = Map.of(
            "invoiceNumber", invoice.number(),
            "invoiceDate", invoice.date(),
            "company", mapCompanyInfo(),
            "customer", mapCustomer(invoice.customer()),
            "items", mapInvoiceItems(invoice.items()),
            "subtotal", invoice.subtotal().amount(),
            "taxRate", invoice.taxRate(),
            "tax", invoice.tax().amount(),
            "total", invoice.total().amount(),
            "paymentInstructions", invoice.paymentInstructions()
        );
        
        Result<String> htmlResult = templateEngine.render(
            "invoices/invoice",
            variables
        );
        
        if (htmlResult.isError()) {
            return htmlResult.mapError();
        }
        
        // Generate PDF from HTML
        Result<byte[]> pdfResult = pdfGenerator.fromHtml(htmlResult.get());
        
        if (pdfResult.isError()) {
            return pdfResult.mapError();
        }
        
        // Store PDF
        String key = String.format(
            "invoices/%s/%s/%s.pdf",
            invoice.date().getYear(),
            invoice.date().getMonthValue(),
            invoice.number()
        );
        
        FileMetadata metadata = FileMetadata.builder()
            .contentType("application/pdf")
            .contentDisposition("attachment; filename=\"invoice-" + invoice.number() + ".pdf\"")
            .build();
        
        return fileStorage.store(key, pdfResult.get(), metadata);
    }
}
```

### Report PDF Service

```java
@Service
public class ReportPdfService {
    
    private final DocumentGenerator pdfGenerator;
    
    public Result<byte[]> generateSalesReport(SalesReport report) {
        DocumentRequest request = DocumentRequest.builder()
            .content(buildReportHtml(report))
            .format(DocumentFormat.PDF)
            .metadata(new DocumentMetadata(
                "Sales Report - " + report.period(),
                "Report System",
                Optional.of("Monthly sales analysis"),
                Optional.of("sales, report, analytics"),
                LocalDateTime.now()
            ))
            .option("pageSize", "A4")
            .option("orientation", "portrait")
            .option("margins", Map.of(
                "top", 20,
                "right", 20,
                "bottom", 20,
                "left", 20
            ))
            .build();
        
        return pdfGenerator.generate(request);
    }
    
    private String buildReportHtml(SalesReport report) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; }
                    h1 { color: #333; }
                    table { width: 100%; border-collapse: collapse; }
                    th, td { border: 1px solid #ddd; padding: 8px; }
                    th { background-color: #4CAF50; color: white; }
                </style>
            </head>
            <body>
                <h1>Sales Report</h1>
                <p>Period: %s</p>
                <table>
                    <thead>
                        <tr>
                            <th>Product</th>
                            <th>Units Sold</th>
                            <th>Revenue</th>
                        </tr>
                    </thead>
                    <tbody>
                        %s
                    </tbody>
                </table>
            </body>
            </html>
            """.formatted(
                report.period(),
                report.items().stream()
                    .map(item -> "<tr><td>%s</td><td>%d</td><td>$%.2f</td></tr>"
                        .formatted(item.product(), item.units(), item.revenue()))
                    .collect(Collectors.joining("\n"))
            );
    }
}
```

---

## 📊 Excel Generation

### ExcelGenerator Interface

```java
public interface ExcelGenerator {
    
    /**
     * Cria workbook.
     */
    Workbook createWorkbook();
    
    /**
     * Adiciona sheet.
     */
    Sheet addSheet(Workbook workbook, String sheetName);
    
    /**
     * Adiciona header row.
     */
    Row addHeaderRow(Sheet sheet, List<String> headers);
    
    /**
     * Adiciona data row.
     */
    Row addDataRow(Sheet sheet, int rowNum, List<Object> data);
    
    /**
     * Converte workbook para bytes.
     */
    Result<byte[]> toBytes(Workbook workbook);
}
```

### Product Export Service

```java
@Service
public class ProductExportService {
    
    private final ExcelGenerator excelGenerator;
    private final ProductRepository productRepository;
    
    public Result<byte[]> exportProductsToExcel() {
        // Get all products
        List<Product> products = productRepository.findAll();
        
        // Create workbook
        Workbook workbook = excelGenerator.createWorkbook();
        Sheet sheet = excelGenerator.addSheet(workbook, "Products");
        
        // Add header
        List<String> headers = List.of(
            "ID", "Name", "Category", "Price", "In Stock", "Created At"
        );
        excelGenerator.addHeaderRow(sheet, headers);
        
        // Add data rows
        int rowNum = 1;
        for (Product product : products) {
            List<Object> row = List.of(
                product.id().value(),
                product.name(),
                product.category().name(),
                product.price().amount(),
                product.inStock() ? "Yes" : "No",
                product.createdAt().toString()
            );
            excelGenerator.addDataRow(sheet, rowNum++, row);
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
        }
        
        return excelGenerator.toBytes(workbook);
    }
}
```

### Multi-Sheet Excel

```java
@Service
public class OrderReportExcelService {
    
    private final ExcelGenerator excelGenerator;
    
    public Result<byte[]> generateOrderReport(YearMonth month) {
        Workbook workbook = excelGenerator.createWorkbook();
        
        // Sheet 1: Summary
        addSummarySheet(workbook, month);
        
        // Sheet 2: Orders
        addOrdersSheet(workbook, month);
        
        // Sheet 3: Top Products
        addTopProductsSheet(workbook, month);
        
        return excelGenerator.toBytes(workbook);
    }
    
    private void addSummarySheet(Workbook workbook, YearMonth month) {
        Sheet sheet = excelGenerator.addSheet(workbook, "Summary");
        
        OrderStats stats = calculateStats(month);
        
        // Key-Value pairs
        int row = 0;
        addDataRow(sheet, row++, List.of("Metric", "Value"));
        addDataRow(sheet, row++, List.of("Total Orders", stats.totalOrders()));
        addDataRow(sheet, row++, List.of("Total Revenue", stats.totalRevenue()));
        addDataRow(sheet, row++, List.of("Average Order Value", stats.averageOrderValue()));
        addDataRow(sheet, row++, List.of("New Customers", stats.newCustomers()));
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }
    
    private void addOrdersSheet(Workbook workbook, YearMonth month) {
        Sheet sheet = excelGenerator.addSheet(workbook, "Orders");
        
        List<Order> orders = orderRepository.findByMonth(month);
        
        // Header
        excelGenerator.addHeaderRow(sheet, List.of(
            "Order ID", "Customer", "Date", "Status", "Total"
        ));
        
        // Data
        int rowNum = 1;
        for (Order order : orders) {
            excelGenerator.addDataRow(sheet, rowNum++, List.of(
                order.id().value(),
                order.customerName(),
                order.createdAt().toString(),
                order.status().name(),
                order.total().amount()
            ));
        }
    }
}
```

---

## 📋 CSV Export

### CsvGenerator Interface

```java
public interface CsvGenerator {
    
    /**
     * Gera CSV.
     */
    Result<byte[]> generate(CsvData data);
}

public record CsvData(
    List<String> headers,
    List<List<String>> rows,
    CsvOptions options
) {}

public record CsvOptions(
    char delimiter,
    char quoteChar,
    boolean includeHeader
) {
    public static CsvOptions defaults() {
        return new CsvOptions(',', '"', true);
    }
}
```

### CSV Export Service

```java
@Service
public class UserExportService {
    
    private final CsvGenerator csvGenerator;
    private final UserRepository userRepository;
    
    public Result<byte[]> exportUsersToCsv() {
        List<User> users = userRepository.findAll();
        
        List<String> headers = List.of(
            "ID", "Email", "Name", "Status", "Created At"
        );
        
        List<List<String>> rows = users.stream()
            .map(user -> List.of(
                user.id().value(),
                user.email(),
                user.name(),
                user.status().name(),
                user.createdAt().toString()
            ))
            .toList();
        
        CsvData csvData = new CsvData(
            headers,
            rows,
            CsvOptions.defaults()
        );
        
        return csvGenerator.generate(csvData);
    }
    
    public Result<byte[]> exportOrdersToCsv(YearMonth month) {
        List<Order> orders = orderRepository.findByMonth(month);
        
        List<String> headers = List.of(
            "Order ID", "Customer ID", "Customer Name", "Date", 
            "Status", "Items", "Subtotal", "Tax", "Total"
        );
        
        List<List<String>> rows = orders.stream()
            .map(order -> List.of(
                order.id().value(),
                order.customerId().value(),
                order.customerName(),
                order.createdAt().toString(),
                order.status().name(),
                String.valueOf(order.items().size()),
                String.valueOf(order.subtotal().amount()),
                String.valueOf(order.tax().amount()),
                String.valueOf(order.total().amount())
            ))
            .toList();
        
        CsvData csvData = new CsvData(headers, rows, CsvOptions.defaults());
        
        return csvGenerator.generate(csvData);
    }
}
```

---

## 📥 Document Download Controller

### REST API

```java
@RestController
@RequestMapping("/api/v1/exports")
public class ExportController {
    
    private final ProductExportService productExportService;
    private final OrderReportExcelService orderReportService;
    private final UserExportService userExportService;
    
    @GetMapping("/products.xlsx")
    public ResponseEntity<byte[]> exportProducts() {
        Result<byte[]> result = productExportService.exportProductsToExcel();
        
        if (result.isError()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename("products.xlsx")
                .build()
        );
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(result.get());
    }
    
    @GetMapping("/orders/{year}/{month}")
    public ResponseEntity<byte[]> exportOrders(
        @PathVariable int year,
        @PathVariable int month
    ) {
        YearMonth yearMonth = YearMonth.of(year, month);
        Result<byte[]> result = orderReportService.generateOrderReport(yearMonth);
        
        if (result.isError()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        
        String filename = String.format("orders-%d-%02d.xlsx", year, month);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename(filename)
                .build()
        );
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(result.get());
    }
    
    @GetMapping("/users.csv")
    public ResponseEntity<byte[]> exportUsersCsv() {
        Result<byte[]> result = userExportService.exportUsersToCsv();
        
        if (result.isError()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename("users.csv")
                .build()
        );
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(result.get());
    }
}
```

---

## 🔄 Async Export

### Background Export Service

```java
@Service
public class AsyncExportService {
    
    private final ExcelGenerator excelGenerator;
    private final FileStorage fileStorage;
    private final QueuePublisher queuePublisher;
    
    public Result<ExportJobId> startExport(ExportRequest request) {
        ExportJobId jobId = ExportJobId.generate();
        
        // Queue export job
        QueueMessage message = QueueMessage.builder()
            .body(serializeRequest(request))
            .attribute("jobId", jobId.value())
            .attribute("type", "export")
            .build();
        
        return queuePublisher.send("export-queue", message)
            .map(messageId -> jobId);
    }
    
    @Scheduled(fixedDelay = 1000)
    public void processExportJobs() {
        Result<List<ReceivedMessage>> result = queueConsumer.receive(
            "export-queue",
            ReceiveOptions.defaults()
        );
        
        result.ifOk(messages -> 
            messages.forEach(this::processExport)
        );
    }
    
    private void processExport(ReceivedMessage message) {
        try {
            ExportRequest request = parseRequest(message.body());
            
            // Generate export
            Result<byte[]> exportResult = generateExport(request);
            
            if (exportResult.isError()) {
                log.error("Export failed")
                    .error(exportResult.getError())
                    .log();
                return;
            }
            
            // Store file
            String key = "exports/" + request.jobId().value() + ".xlsx";
            fileStorage.store(key, exportResult.get());
            
            // Notify user (email or notification)
            notifyExportReady(request.userId(), key);
            
            // Delete from queue
            queueConsumer.delete("export-queue", message.receiptHandle());
            
        } catch (Exception e) {
            log.error("Failed to process export")
                .exception(e)
                .log();
        }
    }
}

public record ExportRequest(
    ExportJobId jobId,
    UserId userId,
    ExportType type,
    Map<String, Object> parameters
) {}
```

---

## 🧪 Testing

### Mock Document Generator

```java
public class MockDocumentGenerator implements DocumentGenerator {
    
    private byte[] mockPdf = "mock pdf content".getBytes();
    
    @Override
    public Result<byte[]> fromHtml(String html) {
        return Result.ok(mockPdf);
    }
    
    @Override
    public Result<byte[]> fromTemplate(
        String templateName,
        Map<String, Object> variables
    ) {
        return Result.ok(mockPdf);
    }
    
    @Override
    public Result<byte[]> generate(DocumentRequest request) {
        return Result.ok(mockPdf);
    }
    
    public void setMockPdf(byte[] mockPdf) {
        this.mockPdf = mockPdf;
    }
}
```

### Test Example

```java
class InvoicePdfServiceTest {
    
    private MockDocumentGenerator pdfGenerator;
    private MockTemplateEngine templateEngine;
    private MockFileStorage fileStorage;
    private InvoicePdfService invoiceService;
    
    @BeforeEach
    void setUp() {
        pdfGenerator = new MockDocumentGenerator();
        templateEngine = new MockTemplateEngine();
        fileStorage = new MockFileStorage();
        invoiceService = new InvoicePdfService(
            pdfGenerator,
            templateEngine,
            fileStorage
        );
        
        templateEngine.mockTemplate(
            "invoices/invoice",
            "<html>Invoice ${invoiceNumber}</html>"
        );
    }
    
    @Test
    void shouldGenerateInvoicePdf() {
        // Given
        Invoice invoice = Invoice.create(customer, items);
        
        // When
        Result<FileLocation> result = invoiceService.generateInvoicePdf(invoice);
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        FileLocation location = result.get();
        assertThat(location.key()).contains("invoices");
        assertThat(location.key()).contains(invoice.number());
        
        // Verify file stored
        assertThat(fileStorage.exists(location.key())).isTrue();
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Set proper content type
headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

// ✅ Use Content-Disposition para filename
headers.setContentDisposition(
    ContentDisposition.attachment().filename("report.pdf").build()
);

// ✅ Auto-size Excel columns
for (int i = 0; i < headers.size(); i++) {
    sheet.autoSizeColumn(i);
}

// ✅ Add metadata para PDFs
.metadata(new DocumentMetadata(title, author, subject, keywords, now));

// ✅ Use async para large exports
asyncExportService.startExport(request);
```

### ❌ DON'T

```java
// ❌ NÃO gere documentos grandes síncronamente
byte[] pdf = generateHugeReport();  // ❌ Timeout!

// ❌ NÃO mantenha workbooks em memória
Workbook wb = createWorkbook();
// ... forget to close

// ❌ NÃO exponha dados sensíveis
exportService.exportAllUsersWithPasswords();  // ❌ Security!

// ❌ NÃO ignore file cleanup
// Close streams, delete temp files

// ❌ NÃO use encoding errado
writer.write(data);  // ❌ Use UTF-8
```

---

## Ver Também

- [PDF Adapter (iText)](../../../commons-adapters-pdf-itext/) - PDF implementation
- [Excel Adapter (POI)](../../../commons-adapters-excel-poi/) - Excel implementation
- [Templates](./templates.md) - HTML templates for PDFs
- [Files](./files.md) - File storage
