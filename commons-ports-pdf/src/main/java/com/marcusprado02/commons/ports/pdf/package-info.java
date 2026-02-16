/**
 * PDF generation and manipulation port.
 *
 * <p>This package provides a platform-agnostic abstraction for PDF generation with support for:
 *
 * <ul>
 *   <li><b>Document Creation</b>: Metadata, page size, margins
 *   <li><b>Content Elements</b>: Text, paragraphs, images, tables
 *   <li><b>Layout Control</b>: Alignment, spacing, page breaks
 *   <li><b>Digital Signatures</b>: PKCS#12 keystores with visible/invisible signatures
 *   <li><b>Error Handling</b>: Result pattern for type-safe error handling
 * </ul>
 *
 * <h2>Core Components</h2>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.ports.pdf.PdfPort} - Main interface for PDF operations
 *   <li>{@link com.marcusprado02.commons.ports.pdf.PdfDocument} - Document with metadata and
 *       content
 *   <li>{@link com.marcusprado02.commons.ports.pdf.PdfElement} - Content elements (text, image,
 *       table)
 *   <li>{@link com.marcusprado02.commons.ports.pdf.PdfSignature} - Digital signature configuration
 *   <li>{@link com.marcusprado02.commons.ports.pdf.PageSize} - Standard and custom page sizes
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Create document with content
 * PdfDocument document = PdfDocument.builder()
 *     .title("Invoice #12345")
 *     .author("Acme Corp")
 *     .pageSize(PageSize.A4)
 *     .margins(PdfDocument.Margins.standard())
 *     .addElements(
 *         new PdfElement.Text("INVOICE", 24f, true, false, null),
 *         new PdfElement.Space(20f),
 *         new PdfElement.Paragraph("Date: 2026-02-16", Alignment.RIGHT),
 *         new PdfElement.Table(
 *             List.of("Item", "Quantity", "Price"),
 *             List.of(
 *                 List.of("Widget A", "10", "$100"),
 *                 List.of("Widget B", "5", "$50")
 *             )
 *         )
 *     )
 *     .build();
 *
 * // Generate PDF
 * try (OutputStream out = Files.newOutputStream(Path.of("invoice.pdf"))) {
 *     Result<Void> result = pdfPort.generate(document, out);
 *     if (result.isFailure()) {
 *         System.err.println("Failed: " + result.getError());
 *     }
 * }
 *
 * // Generate signed PDF
 * PdfSignature signature = PdfSignature.builder()
 *     .keystoreData(keystoreStream)
 *     .keystorePassword("password")
 *     .keyAlias("mykey")
 *     .reason("Official invoice")
 *     .location("New York, USA")
 *     .signatureField(1, 400, 50, 200, 100)
 *     .build();
 *
 * Result<Void> result = pdfPort.generateAndSign(document, signature, outputStream);
 * }</pre>
 *
 * <h2>Implementation Notes</h2>
 *
 * <ul>
 *   <li>Output streams are not closed by the port - caller manages lifecycle
 *   <li>All coordinates and dimensions are in points (1/72 inch)
 *   <li>Page numbering is 1-based for signature fields
 *   <li>Digital signatures require PKCS#12 keystore format
 *   <li>Implementations should be thread-safe for concurrent use
 * </ul>
 *
 * @since 0.1.0
 */
package com.marcusprado02.commons.ports.pdf;
