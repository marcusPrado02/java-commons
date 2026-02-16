# Commons Adapters - iText PDF

iText 7 implementation of the PDF generation port.

## Overview

This adapter provides full-featured PDF generation using **iText 7**, supporting:

- **Text & Paragraphs**: Rich text formatting with fonts, colors, alignment
- **Images**: PNG, JPEG with scaling and positioning
- **Tables**: Multi-column tables with headers and styling
- **Digital Signatures**: PKCS#12 keystores with visible/invisible signatures
- **Compression**: Multiple compression modes for optimized file size
- **Encryption**: Password protection with 40/128/256-bit encryption
- **PDF Versions**: Support for PDF 1.4 through 2.0

## Installation

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-pdf-itext</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Dependencies

- **iText 7.2.5**: PDF library
- **Bouncy Castle 1.70**: Cryptography provider
- **commons-ports-pdf**: Port interface
- **commons-kernel-result**: Result pattern
- **commons-kernel-errors**: Problem API

## Quick Start

### Basic Usage

```java
// Create adapter with default configuration
PdfPort pdfPort = new ITextPdfAdapter();

// Create document
PdfDocument document = PdfDocument.builder()
    .title("My First PDF")
    .author("John Doe")
    .addElements(
        new PdfElement.Text("Hello, iText!", 24f, true, false, null),
        new PdfElement.Space(20f),
        new PdfElement.Paragraph("This is my first PDF generated with iText 7.")
    )
    .build();

// Generate PDF
try (OutputStream out = Files.newOutputStream(Path.of("output.pdf"))) {
    Result<Void> result = pdfPort.generate(document, out);

    result.ifSuccess(v -> System.out.println("PDF created successfully!"))
          .ifFailure(error -> System.err.println("Error: " + error.getDetail()));
}
```

### Custom Configuration

```java
// Compressed PDF for smaller file size
ITextConfiguration config = ITextConfiguration.compressed();
PdfPort pdfPort = new ITextPdfAdapter(config);

// Encrypted PDF
ITextConfiguration encrypted = ITextConfiguration.encrypted(
    "user_password",   // Password to open document
    "owner_password"   // Password for full access
);
PdfPort securePdfPort = new ITextPdfAdapter(encrypted);

// PDF 2.0 with custom settings
ITextConfiguration custom = ITextConfiguration.builder()
    .pdfVersion("2.0")
    .compressContent(true)
    .fullCompressionMode(true)
    .bufferSize(16384)
    .build();
```

## Configuration Options

### ITextConfiguration

| Property              | Type    | Default | Description                           |
|-----------------------|---------|---------|---------------------------------------|
| `compressContent`     | boolean | `true`  | Compress PDF content streams          |
| `fullCompressionMode` | boolean | `false` | Use full compression (smaller files)  |
| `pdfVersion`          | String  | `"1.7"` | PDF version (1.4 - 2.0)               |
| `userPassword`        | String  | `null`  | Password to open document             |
| `ownerPassword`       | String  | `null`  | Password for full permissions         |
| `encryptionBits`      | int     | `0`     | Encryption strength (0/40/128/256)    |
| `bufferSize`          | int     | `8192`  | Output buffer size in bytes           |

### Preset Configurations

```java
// Default configuration
ITextConfiguration.defaultConfig();

// Maximum compression
ITextConfiguration.compressed();

// PDF 2.0
ITextConfiguration.pdf20();

// 256-bit encrypted
ITextConfiguration.encrypted(userPwd, ownerPwd);
```

## Features

### Text Formatting

```java
// Styled text
new PdfElement.Text("Bold Title", 20f, true, false, null);
new PdfElement.Text("Italic Text", 12f, false, true, null);
new PdfElement.Text("Colored Text", 14f, false, false, Color.BLUE);

// Paragraph with alignment
new PdfElement.Paragraph(
    "Lorem ipsum dolor sit amet...",
    12f,
    PdfElement.Alignment.JUSTIFIED,
    1.5f  // Line spacing
);
```

### Images

```java
// Load image
try (InputStream imageStream = Files.newInputStream(Path.of("logo.png"))) {

    // Image with specific dimensions
    new PdfElement.Image(imageStream, 200f, 100f, PdfElement.Alignment.CENTER);

    // Image with auto-scaling (preserves aspect ratio)
    new PdfElement.Image(imageStream, 300f, null, PdfElement.Alignment.LEFT);
}
```

### Tables

```java
// Simple table
List<String> headers = List.of("Name", "Age", "City");
List<List<String>> rows = List.of(
    List.of("Alice", "30", "New York"),
    List.of("Bob", "25", "Los Angeles"),
    List.of("Charlie", "35", "Chicago")
);

new PdfElement.Table(headers, rows);

// Table with custom styling
new PdfElement.Table(
    headers,
    rows,
    new float[]{2f, 1f, 1.5f},        // Column widths (relative)
    new Color(100, 150, 200)          // Header background color
);
```

### Digital Signatures

#### Invisible Signature

```java
try (InputStream keystoreStream = Files.newInputStream(Path.of("keystore.p12"))) {
    PdfSignature signature = PdfSignature.builder()
        .keystoreData(keystoreStream)
        .keystorePassword("password123")
        .keyAlias("my-signing-key")
        .reason("Document approval")
        .location("San Francisco, CA")
        .contactInfo("signer@example.com")
        .build();

    Result<Void> result = pdfPort.generateAndSign(document, signature, outputStream);
}
```

#### Visible Signature with Image

```java
try (InputStream keystoreStream = Files.newInputStream(Path.of("keystore.p12"));
     InputStream signatureImage = Files.newInputStream(Path.of("signature.png"))) {

    // Create signature field on page 1
    PdfSignature.SignatureField field = new PdfSignature.SignatureField(
        1,              // Page number (1-based)
        400,            // X coordinate from left edge
        50,             // Y coordinate from bottom edge
        200,            // Width
        100,            // Height
        signatureImage  // Signature image
    );

    PdfSignature signature = PdfSignature.builder()
        .keystoreData(keystoreStream)
        .keystorePassword("password123")
        .keyAlias("my-signing-key")
        .reason("Official document")
        .location("New York, USA")
        .signatureField(field)
        .build();

    pdfPort.generateAndSign(document, signature, outputStream);
}
```

## Complete Examples

### Invoice Generation

```java
PdfPort pdfPort = new ITextPdfAdapter(ITextConfiguration.compressed());

// Invoice header
List<PdfElement> elements = new ArrayList<>();
elements.add(new PdfElement.Text("INVOICE", 28f, true, false, null));
elements.add(new PdfElement.Space(10f));
elements.add(new PdfElement.Paragraph("Invoice #INV-2026-001", PdfElement.Alignment.RIGHT));
elements.add(new PdfElement.Paragraph("Date: February 16, 2026", PdfElement.Alignment.RIGHT));
elements.add(new PdfElement.Space(20f));

// Customer info
elements.add(new PdfElement.Text("Bill To:", 14f, true, false, null));
elements.add(new PdfElement.Paragraph("Acme Corporation\n123 Main St\nNew York, NY 10001"));
elements.add(new PdfElement.Space(20f));

// Items table
List<String> headers = List.of("Description", "Quantity", "Unit Price", "Total");
List<List<String>> items = List.of(
    List.of("Professional Services", "40 hrs", "$150.00", "$6,000.00"),
    List.of("Software License", "1", "$500.00", "$500.00"),
    List.of("Support (Annual)", "1", "$1,200.00", "$1,200.00")
);

elements.add(new PdfElement.Table(
    headers,
    items,
    new float[]{3f, 1f, 1.5f, 1.5f},
    new Color(220, 230, 240)
));

elements.add(new PdfElement.Space(20f));
elements.add(new PdfElement.Paragraph("Total: $7,700.00", PdfElement.Alignment.RIGHT));

// Create document
PdfDocument invoice = PdfDocument.builder()
    .title("Invoice INV-2026-001")
    .author("Acme Corp")
    .subject("Invoice for Professional Services")
    .pageSize(PageSize.LETTER)
    .margins(PdfDocument.Margins.standard())
    .elements(elements)
    .build();

// Generate PDF
try (OutputStream out = Files.newOutputStream(Path.of("invoice.pdf"))) {
    pdfPort.generate(invoice, out);
}
```

### Report with Images

```java
try (InputStream logoStream = Files.newInputStream(Path.of("logo.png"));
     InputStream chartStream = Files.newInputStream(Path.of("chart.png"))) {

    PdfDocument report = PdfDocument.builder()
        .title("Quarterly Report Q1 2026")
        .author("Finance Team")
        .pageSize(PageSize.A4)
        .addElements(
            // Header with logo
            new PdfElement.Image(logoStream, 150f, 75f, PdfElement.Alignment.CENTER),
            new PdfElement.Space(20f),
            new PdfElement.Text("Q1 2026 Financial Report", 24f, true, false, null),
            new PdfElement.Space(30f),

            // Executive summary
            new PdfElement.Text("Executive Summary", 18f, true, false, null),
            new PdfElement.Paragraph(
                "Revenue increased by 25% compared to Q1 2025, driven by strong "
                + "performance in our cloud services division...",
                12f,
                PdfElement.Alignment.JUSTIFIED,
                1.5f
            ),
            new PdfElement.Space(20f),

            // Chart
            new PdfElement.Text("Revenue Trend", 16f, true, false, null),
            new PdfElement.Space(10f),
            new PdfElement.Image(chartStream, 400f, 250f, PdfElement.Alignment.CENTER),

            // Page break for next section
            new PdfElement.PageBreak(),

            // Detailed analysis on page 2
            new PdfElement.Text("Detailed Analysis", 18f, true, false, null),
            new PdfElement.Paragraph("...")
        )
        .build();

    pdfPort.generate(report, outputStream);
}
```

### Signed Contract

```java
// Create contract document
PdfDocument contract = PdfDocument.builder()
    .title("Service Agreement")
    .author("Legal Department")
    .addElements(
        new PdfElement.Text("SERVICE AGREEMENT", 20f, true, false, null),
        new PdfElement.Space(20f),
        new PdfElement.Paragraph("This Service Agreement (\"Agreement\") is entered into..."),
        // ... contract terms ...
        new PdfElement.Space(50f),
        new PdfElement.Text("Authorized Signature:", 12f, true, false, null)
    )
    .build();

// Sign the contract
try (InputStream keystoreStream = Files.newInputStream(Path.of("company.p12"))) {
    PdfSignature signature = PdfSignature.builder()
        .keystoreData(keystoreStream)
        .keystorePassword(System.getenv("KEYSTORE_PASSWORD"))
        .keyAlias("company-signing-key")
        .reason("Authorized signature")
        .location("Corporate Office")
        .signatureField(1, 100, 100, 300, 100)
        .build();

    try (OutputStream out = Files.newOutputStream(Path.of("signed-contract.pdf"))) {
        Result<Void> result = pdfPort.generateAndSign(contract, signature, out);

        result.ifFailure(error -> {
            log.error("Failed to sign contract: {}", error.getDetail());
            // Handle error
        });
    }
}
```

## Creating Signing Certificates

### Self-Signed Certificate (Testing Only)

```bash
keytool -genkeypair \
    -alias my-signing-key \
    -keyalg RSA \
    -keysize 2048 \
    -sigalg SHA256withRSA \
    -validity 365 \
    -keystore keystore.p12 \
    -storetype PKCS12 \
    -storepass password123 \
    -keypass password123 \
    -dname "CN=John Doe, OU=Engineering, O=Acme Corp, L=New York, ST=NY, C=US"
```

### Production Certificate

For production, obtain a code signing certificate from a trusted Certificate Authority (CA):

- DigiCert
- GlobalSign
- Entrust
- Sectigo

## Error Handling

All operations return `Result<Void>` for type-safe error handling:

```java
Result<Void> result = pdfPort.generate(document, outputStream);

// Pattern matching
String message = result.match(
    success -> "PDF generated successfully",
    error -> "Failed: " + error.getDetail()
);

// Conditional execution
result
    .ifSuccess(v -> log.info("PDF created"))
    .ifFailure(error -> {
        log.error("Error: {}", error.getDetail());
        if ("PDF_GENERATION_ERROR".equals(error.getCode().value())) {
            // Handle specific error
        }
    });

// Exception throwing
result.orElseThrow(error ->
    new RuntimeException("PDF generation failed: " + error.getDetail())
);
```

### Error Codes

| Code                      | Description                        |
|---------------------------|------------------------------------|
| `PDF_GENERATION_ERROR`    | Failed to generate PDF document    |
| `PDF_SIGNING_ERROR`       | Failed to apply digital signature  |

## Performance Tips

### Memory Optimization

```java
// Use compressed configuration for large documents
ITextConfiguration config = ITextConfiguration.builder()
    .compressContent(true)
    .fullCompressionMode(true)
    .bufferSize(16384)  // Larger buffer for better throughput
    .build();

PdfPort pdfPort = new ITextPdfAdapter(config);
```

### Large Images

Resize images before adding to PDF:

```java
// Instead of adding full-resolution image
// Resize to appropriate dimensions for PDF

float maxWidth = 500f;  // Max width in points
float maxHeight = 400f;

new PdfElement.Image(imageStream, maxWidth, maxHeight);
```

### Table Performance

For very large tables, consider splitting into multiple pages:

```java
// Split large dataset into chunks
int pageSize = 50;  // Rows per page

for (int page = 0; page < totalRows; page += pageSize) {
    List<List<String>> pageRows = allRows.subList(
        page,
        Math.min(page + pageSize, totalRows)
    );

    elements.add(new PdfElement.Table(headers, pageRows));

    if (page + pageSize < totalRows) {
        elements.add(new PdfElement.PageBreak());
    }
}
```

## Thread Safety

`ITextPdfAdapter` instances are **thread-safe** and can be reused:

```java
// Create once, reuse for all requests
private static final PdfPort PDF_PORT = new ITextPdfAdapter(
    ITextConfiguration.compressed()
);

// Use from multiple threads
CompletableFuture.allOf(
    CompletableFuture.runAsync(() -> PDF_PORT.generate(doc1, out1)),
    CompletableFuture.runAsync(() -> PDF_PORT.generate(doc2, out2)),
    CompletableFuture.runAsync(() -> PDF_PORT.generate(doc3, out3))
).join();
```

## iText 7 vs iText 5

This adapter uses **iText 7**, which has significant improvements over iText 5:

| Feature                | iText 7                 | iText 5             |
|------------------------|-------------------------|---------------------|
| **API Design**         | Clean, fluent           | Legacy, verbose     |
| **PDF/A Support**      | Built-in                | Separate library    |
| **Performance**        | Faster                  | Slower              |
| **Memory Usage**       | Lower                   | Higher              |
| **License**            | AGPL / Commercial       | AGPL / Commercial   |

## License

iText 7 is dual-licensed:

- **AGPL**: Free for open-source projects
- **Commercial**: Required for closed-source applications

See [iText Licensing](https://itextpdf.com/en/how-buy) for details.

## Related Modules

- **commons-ports-pdf**: Port interface
- **commons-kernel-result**: Result pattern
- **commons-kernel-errors**: Problem API

## Troubleshooting

### Problem: "PdfAConformanceException"

**Solution**: Use PDF 1.7 or lower for signed documents:

```java
ITextConfiguration config = ITextConfiguration.builder()
    .pdfVersion("1.7")
    .build();
```

### Problem: Image not showing

**Solution**: Ensure image format is supported (PNG, JPEG) and InputStream is not closed:

```java
byte[] imageBytes = Files.readAllBytes(Path.of("image.png"));
InputStream imageStream = new ByteArrayInputStream(imageBytes);
new PdfElement.Image(imageStream, 200f, 150f);
```

### Problem: Table overflows page

**Solution**: Let iText handle splitting or manually insert page breaks:

```java
// iText automatically splits tables across pages
// Or manually control:
elements.add(new PdfElement.Table(headers, firstPageRows));
elements.add(new PdfElement.PageBreak());
elements.add(new PdfElement.Table(headers, secondPageRows));
```

## Support

For issues specific to this adapter, consult:

- [iText 7 Documentation](https://kb.itextpdf.com/home/it7kb)
- [iText GitHub](https://github.com/itext/itext7)
- Commons Platform documentation
