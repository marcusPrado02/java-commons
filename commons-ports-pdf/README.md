# Commons Ports - PDF Generation

Platform-agnostic port interface for PDF document generation and manipulation.

## Overview

This module provides a clean abstraction for PDF generation supporting:

- **Document Creation**: Metadata, page sizes, margins
- **Content Elements**: Text, paragraphs, images, tables
- **Layout Control**: Alignment, spacing, page breaks
- **Digital Signatures**: PKCS#12 keystores with visible/invisible signatures
- **Error Handling**: Result pattern for type-safe errors

## Installation

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-pdf</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Core Concepts

### PdfPort Interface

Main interface for PDF operations:

```java
public interface PdfPort {
    Result<Void> generate(PdfDocument document, OutputStream output);
    Result<Void> generateAndSign(PdfDocument document, PdfSignature signature, OutputStream output);
    boolean supportsSignatures();
    String getPdfVersion();
}
```

### PdfDocument

Immutable value object representing a PDF document:

```java
PdfDocument document = PdfDocument.builder()
    .title("Annual Report 2026")
    .author("John Doe")
    .subject("Financial Results")
    .keywords("finance, annual, report")
    .pageSize(PageSize.A4)
    .margins(PdfDocument.Margins.standard())
    .addElements(/* content elements */)
    .build();
```

### PdfElement

Sealed interface for document content:

- **Text**: Styled text with font size, bold, italic, color
- **Paragraph**: Multi-line text with alignment and leading
- **Image**: Images from InputStream with dimension control
- **Table**: Tabular data with headers and custom styling
- **Space**: Vertical spacing
- **PageBreak**: Force page break

## Usage Examples

### Simple Document

```java
PdfDocument document = PdfDocument.builder()
    .title("Hello World")
    .author("Test User")
    .addElements(
        new PdfElement.Text("Hello, PDF!", 24f),
        new PdfElement.Space(20f),
        new PdfElement.Paragraph(
            "This is a simple paragraph with justified text.",
            PdfElement.Alignment.JUSTIFIED
        )
    )
    .build();

try (OutputStream out = Files.newOutputStream(Path.of("hello.pdf"))) {
    Result<Void> result = pdfPort.generate(document, out);
    result.ifFailure(error -> System.err.println("Error: " + error));
}
```

### Document with Table

```java
List<String> headers = List.of("Product", "Quantity", "Price", "Total");
List<List<String>> rows = List.of(
    List.of("Widget A", "10", "$5.00", "$50.00"),
    List.of("Widget B", "5", "$10.00", "$50.00"),
    List.of("Widget C", "3", "$15.00", "$45.00")
);

PdfDocument document = PdfDocument.builder()
    .title("Invoice #12345")
    .pageSize(PageSize.LETTER)
    .addElements(
        new PdfElement.Text("INVOICE", 28f, true, false, null),
        new PdfElement.Space(15f),
        new PdfElement.Paragraph("Date: 2026-02-16", PdfElement.Alignment.RIGHT),
        new PdfElement.Space(10f),
        new PdfElement.Table(
            headers,
            rows,
            new float[]{2f, 1f, 1f, 1f}, // Column widths
            new Color(200, 220, 255) // Header background
        )
    )
    .build();

pdfPort.generate(document, outputStream);
```

### Document with Image

```java
try (InputStream imageStream = Files.newInputStream(Path.of("logo.png"))) {
    PdfDocument document = PdfDocument.builder()
        .title("Company Report")
        .addElements(
            new PdfElement.Image(imageStream, 200f, 100f, PdfElement.Alignment.CENTER),
            new PdfElement.Space(20f),
            new PdfElement.Text("Company Report 2026", 24f, true, false, null)
        )
        .build();

    pdfPort.generate(document, outputStream);
}
```

### Signed Document

```java
// Load keystore
InputStream keystoreStream = Files.newInputStream(Path.of("keystore.p12"));

PdfSignature signature = PdfSignature.builder()
    .keystoreData(keystoreStream)
    .keystorePassword("keystorePassword")
    .keyAlias("mySigningKey")
    .reason("Official document")
    .location("New York, USA")
    .contactInfo("signer@example.com")
    .signatureField(1, 400, 50, 200, 100) // Page 1, position and size
    .build();

Result<Void> result = pdfPort.generateAndSign(document, signature, outputStream);
```

### Custom Page Size and Margins

```java
// Custom page size
PageSize customSize = PageSize.custom(500, 700);

// Landscape orientation
PageSize landscape = PageSize.A4.rotate();

// Custom margins
PdfDocument.Margins narrowMargins = new PdfDocument.Margins(
    36f,  // top
    36f,  // right
    36f,  // bottom
    36f   // left
);

PdfDocument document = PdfDocument.builder()
    .pageSize(landscape)
    .margins(narrowMargins)
    .build();
```

### Multi-Page Document

```java
PdfDocument document = PdfDocument.builder()
    .title("Multi-Page Report")
    .addElements(
        new PdfElement.Text("Chapter 1", 20f, true, false, null),
        new PdfElement.Paragraph("Content of chapter 1..."),
        new PdfElement.Space(50f),

        new PdfElement.PageBreak(), // Force new page

        new PdfElement.Text("Chapter 2", 20f, true, false, null),
        new PdfElement.Paragraph("Content of chapter 2...")
    )
    .build();
```

## Page Sizes

Standard page sizes are provided:

| Size      | Dimensions (mm) | Dimensions (points) | Use Case          |
|-----------|-----------------|---------------------|-------------------|
| A4        | 210 × 297       | 595 × 842           | International std |
| A3        | 297 × 420       | 842 × 1191          | Large documents   |
| A5        | 148 × 210       | 420 × 595           | Booklets          |
| LETTER    | 215.9 × 279.4   | 612 × 792           | North America     |
| LEGAL     | 215.9 × 355.6   | 612 × 1008          | Legal documents   |
| TABLOID   | 279.4 × 431.8   | 792 × 1224          | Newspapers        |
| EXECUTIVE | 184.15 × 266.7  | 522 × 756           | Business docs     |

## Text Alignment

```java
public enum Alignment {
    LEFT,       // Left-aligned text
    CENTER,     // Center-aligned text
    RIGHT,      // Right-aligned text
    JUSTIFIED   // Justified text (stretch to margins)
}
```

## Error Handling

All operations return `Result<T>` for type-safe error handling:

```java
Result<Void> result = pdfPort.generate(document, outputStream);

result.match(
    success -> System.out.println("PDF generated successfully"),
    error -> {
        System.err.println("Error: " + error.getTitle());
        System.err.println("Detail: " + error.getDetail());
    }
);

// Or use ifSuccess/ifFailure
result
    .ifSuccess(v -> System.out.println("Success!"))
    .ifFailure(error -> log.error("Failed to generate PDF", error));
```

## Digital Signatures

Requirements for signing PDFs:

1. **PKCS#12 Keystore** (.p12 or .pfx file)
2. **Private Key** with certificate chain
3. **Password** to access the keystore
4. **Key Alias** identifying the signing key

### Creating a Test Keystore

```bash
# Generate self-signed certificate
keytool -genkeypair -alias mykey -keyalg RSA -keysize 2048 \
    -sigalg SHA256withRSA -validity 365 \
    -keystore keystore.p12 -storetype PKCS12 \
    -storepass password -keypass password \
    -dname "CN=Test User, OU=Dev, O=Company, L=City, ST=State, C=US"
```

### Invisible Signature

```java
PdfSignature signature = PdfSignature.builder()
    .keystoreData(keystoreStream)
    .keystorePassword("password")
    .keyAlias("mykey")
    .reason("Document approval")
    .location("Remote")
    // No signatureField = invisible signature
    .build();
```

### Visible Signature with Image

```java
try (InputStream signatureImage = Files.newInputStream(Path.of("signature.png"))) {
    PdfSignature.SignatureField field = new PdfSignature.SignatureField(
        1,              // Page number (1-based)
        400,            // X coordinate from left
        50,             // Y coordinate from bottom
        200,            // Width
        100,            // Height
        signatureImage  // Signature image
    );

    PdfSignature signature = PdfSignature.builder()
        .keystoreData(keystoreStream)
        .keystorePassword("password")
        .keyAlias("mykey")
        .signatureField(field)
        .build();
}
```

## Thread Safety

Implementations of `PdfPort` should be **thread-safe** and support concurrent document generation.
Instances can be shared across threads and used for multiple simultaneous PDF generations.

## Best Practices

### Resource Management

Always use try-with-resources for streams:

```java
try (OutputStream out = Files.newOutputStream(path);
     InputStream keystore = Files.newInputStream(keystorePath)) {
    pdfPort.generate(document, out);
}
```

### Large Documents

For large documents with many elements, consider:

1. **Streaming**: Generate content incrementally if supported by implementation
2. **Pagination**: Break content into smaller chunks with page breaks
3. **Memory**: Monitor heap usage for documents with large images

### Image Handling

```java
// Resize images to fit page
float maxWidth = PageSize.A4.getWidth() - margins.left() - margins.right();
float maxHeight = PageSize.A4.getHeight() - margins.top() - margins.bottom();

new PdfElement.Image(imageStream, maxWidth, maxHeight);

// Preserve aspect ratio by setting only width or height
new PdfElement.Image(imageStream, maxWidth, null);
```

### Table Formatting

```java
// Equal column widths (null)
new PdfElement.Table(headers, rows);

// Custom widths (relative ratios)
float[] widths = {3f, 2f, 2f, 1f}; // First column 3x wider than last
new PdfElement.Table(headers, rows, widths, headerColor);
```

### Metadata

Always provide meaningful metadata:

```java
PdfDocument.builder()
    .title("Document Title")
    .author("Author Name")
    .subject("Brief Description")
    .keywords("keyword1, keyword2, keyword3")
    .creator("Application Name")
```

## Implementation Guide

### Creating an Adapter

Implement the `PdfPort` interface:

```java
public class MyPdfAdapter implements PdfPort {

    @Override
    public Result<Void> generate(PdfDocument document, OutputStream output) {
        try {
            // 1. Initialize PDF library
            // 2. Set document metadata
            // 3. Create pages with margins
            // 4. Render each element
            // 5. Write to output stream
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(Problem.builder()
                .title("PDF Generation Failed")
                .detail(e.getMessage())
                .build());
        }
    }

    @Override
    public Result<Void> generateAndSign(PdfDocument document,
                                        PdfSignature signature,
                                        OutputStream output) {
        // Generate PDF then apply digital signature
    }
}
```

### Element Rendering

Use pattern matching for type-safe element rendering:

```java
private void renderElement(PdfElement element) {
    switch (element) {
        case PdfElement.Text text -> renderText(text);
        case PdfElement.Paragraph para -> renderParagraph(para);
        case PdfElement.Image img -> renderImage(img);
        case PdfElement.Table table -> renderTable(table);
        case PdfElement.Space space -> addVerticalSpace(space.height());
        case PdfElement.PageBreak pb -> addPageBreak();
    }
}
```

## Related Modules

- **commons-adapters-pdf-itext**: iText 7 implementation
- **commons-kernel-result**: Result pattern for error handling
- **commons-kernel-errors**: Problem API for structured errors

## License

See the main project LICENSE file.
