package com.marcusprado02.commons.ports.pdf;

import com.marcusprado02.commons.kernel.result.Result;
import java.io.OutputStream;

/**
 * Port interface for PDF generation and manipulation.
 *
 * <p>Provides a platform-agnostic API for creating PDF documents with text, images, tables, and
 * digital signatures. Implementations should handle:
 *
 * <ul>
 *   <li>Document creation with metadata
 *   <li>Content rendering (text, paragraphs, images, tables)
 *   <li>Page layout and formatting
 *   <li>Digital signatures (optional)
 *   <li>Error handling via Result pattern
 * </ul>
 *
 * <p><b>Example usage:</b>
 *
 * <pre>{@code
 * PdfDocument document = PdfDocument.builder()
 *     .title("Annual Report")
 *     .author("John Doe")
 *     .pageSize(PageSize.A4)
 *     .addElements(
 *         new PdfElement.Text("Report Title", 24f, true, false, null),
 *         new PdfElement.Space(20f),
 *         new PdfElement.Paragraph("This is the content...", Alignment.JUSTIFIED)
 *     )
 *     .build();
 *
 * Result<Void> result = pdfPort.generate(document, outputStream);
 * }</pre>
 *
 * <p><b>Thread Safety:</b> Implementations should be thread-safe for concurrent document
 * generation.
 *
 * @since 0.1.0
 */
public interface PdfPort {

  /**
   * Generates a PDF document and writes it to the provided output stream.
   *
   * <p>The output stream is <b>not</b> closed by this method. The caller is responsible for
   * managing the stream lifecycle.
   *
   * @param document PDF document to generate
   * @param output output stream to write PDF bytes
   * @return Result indicating success or containing error details
   */
  Result<Void> generate(PdfDocument document, OutputStream output);

  /**
   * Generates and digitally signs a PDF document.
   *
   * <p>The document is created with the provided content and then digitally signed using the
   * signature configuration. The signed PDF is written to the output stream.
   *
   * @param document PDF document to generate and sign
   * @param signature signature configuration
   * @param output output stream to write signed PDF bytes
   * @return Result indicating success or containing error details
   */
  Result<Void> generateAndSign(PdfDocument document, PdfSignature signature, OutputStream output);

  /**
   * Checks if this implementation supports digital signatures.
   *
   * @return true if digital signatures are supported
   */
  default boolean supportsSignatures() {
    return true;
  }

  /**
   * Gets the maximum supported PDF version.
   *
   * @return PDF version string (e.g., "1.7", "2.0")
   */
  default String getPdfVersion() {
    return "1.7";
  }
}
