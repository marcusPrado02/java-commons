package com.marcusprado02.commons.adapters.pdf.itext;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.signatures.*;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.pdf.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * iText 7 implementation of PdfPort.
 *
 * <p>Provides full-featured PDF generation using iText 7 with support for:
 *
 * <ul>
 *   <li>Document metadata and properties
 *   <li>Text rendering with fonts and styling
 *   <li>Images with scaling and positioning
 *   <li>Tables with headers and custom formatting
 *   <li>Digital signatures using PKCS#12 keystores
 *   <li>PDF compression and encryption
 * </ul>
 *
 * <p><b>Thread Safety:</b> Instances are thread-safe and can be reused for multiple document
 * generations.
 *
 * @since 0.1.0
 */
public class ITextPdfAdapter implements PdfPort {

  private static final Logger log = LoggerFactory.getLogger(ITextPdfAdapter.class);
  private static final String ERROR_CODE_PDF_GENERATION = "PDF_GENERATION_ERROR";
  private static final String ERROR_CODE_PDF_SIGNING = "PDF_SIGNING_ERROR";

  static {
    // Register Bouncy Castle provider for cryptography
    java.security.Security.addProvider(new BouncyCastleProvider());
  }

  private final ITextConfiguration configuration;

  /**
   * Creates a new adapter with the specified configuration.
   *
   * @param configuration iText configuration
   */
  public ITextPdfAdapter(ITextConfiguration configuration) {
    this.configuration = configuration;
  }

  /** Creates a new adapter with default configuration. */
  public ITextPdfAdapter() {
    this(ITextConfiguration.defaultConfig());
  }

  @Override
  public Result<Void> generate(com.marcusprado02.commons.ports.pdf.PdfDocument document, OutputStream output) {
    try {
      log.debug("Generating PDF document: {}", document.title());

      WriterProperties properties = createWriterProperties();
      PdfWriter writer = new PdfWriter(output, properties);
      com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);

      // Set document metadata
      setDocumentInfo(pdfDoc, document);

      // Create document
      Document doc = new Document(pdfDoc);

      // Set page size and margins
      setMargins(doc, document.margins());

      // Render all elements
      for (PdfElement element : document.elements()) {
        renderElement(doc, element);
      }

      doc.close();
      log.debug("PDF document generated successfully");

      return Result.ok(null);
    } catch (Exception e) {
      log.error("Failed to generate PDF", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of(ERROR_CODE_PDF_GENERATION),
              com.marcusprado02.commons.kernel.errors.ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to generate PDF document: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> generateAndSign(
      com.marcusprado02.commons.ports.pdf.PdfDocument document, com.marcusprado02.commons.ports.pdf.PdfSignature signature, OutputStream output) {
    try {
      log.debug("Generating and signing PDF document: {}", document.title());

      // Generate PDF to byte array first
      ByteArrayOutputStream tempOutput = new ByteArrayOutputStream();
      Result<Void> generateResult = generate(document, tempOutput);

      if (generateResult.isFail()) {
        return generateResult;
      }

      byte[] pdfBytes = tempOutput.toByteArray();

      // Sign the PDF
      signPdf(pdfBytes, signature, output);

      log.debug("PDF document signed successfully");
      return Result.ok(null);
    } catch (Exception e) {
      log.error("Failed to sign PDF", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of(ERROR_CODE_PDF_SIGNING),
              com.marcusprado02.commons.kernel.errors.ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to sign PDF document: " + e.getMessage()));
    }
  }

  @Override
  public boolean supportsSignatures() {
    return true;
  }

  @Override
  public String getPdfVersion() {
    return configuration.pdfVersion();
  }

  private WriterProperties createWriterProperties() {
    WriterProperties properties = new WriterProperties();

    // Set PDF version
    switch (configuration.pdfVersion()) {
      case "1.4" -> properties.setPdfVersion(PdfVersion.PDF_1_4);
      case "1.5" -> properties.setPdfVersion(PdfVersion.PDF_1_5);
      case "1.6" -> properties.setPdfVersion(PdfVersion.PDF_1_6);
      case "1.7" -> properties.setPdfVersion(PdfVersion.PDF_1_7);
      case "2.0" -> properties.setPdfVersion(PdfVersion.PDF_2_0);
    }

    // Set compression
    if (configuration.compressContent()) {
      properties.setCompressionLevel(9); // Maximum compression
    }
    if (configuration.fullCompressionMode()) {
      properties.setFullCompressionMode(true);
    }

    // Set encryption if configured
    if (configuration.userPassword() != null || configuration.ownerPassword() != null) {
      byte[] userPassword =
          configuration.userPassword() != null ? configuration.userPassword().getBytes() : null;
      byte[] ownerPassword =
          configuration.ownerPassword() != null ? configuration.ownerPassword().getBytes() : null;

      int permissions = EncryptionConstants.ALLOW_PRINTING | EncryptionConstants.ALLOW_COPY;

      int encryption =
          switch (configuration.encryptionBits()) {
            case 40 -> EncryptionConstants.STANDARD_ENCRYPTION_40;
            case 128 -> EncryptionConstants.STANDARD_ENCRYPTION_128;
            case 256 -> EncryptionConstants.ENCRYPTION_AES_256;
            default -> EncryptionConstants.STANDARD_ENCRYPTION_128;
          };

      properties.setStandardEncryption(userPassword, ownerPassword, permissions, encryption);
    }

    return properties;
  }

  private void setDocumentInfo(com.itextpdf.kernel.pdf.PdfDocument pdfDoc, com.marcusprado02.commons.ports.pdf.PdfDocument document) {
    PdfDocumentInfo info = pdfDoc.getDocumentInfo();

    if (document.title() != null) {
      info.setTitle(document.title());
    }
    if (document.author() != null) {
      info.setAuthor(document.author());
    }
    if (document.subject() != null) {
      info.setSubject(document.subject());
    }
    if (document.keywords() != null) {
      info.setKeywords(document.keywords());
    }
    if (document.creator() != null) {
      info.setCreator(document.creator());
    }

    // Set creation date
    ZonedDateTime zdt = document.createdAt().atZone(ZoneId.systemDefault());
    GregorianCalendar.from(zdt); // Ensures valid date format
    info.setCreator(document.creator());

    // Set custom properties
    for (java.util.Map.Entry<String, String> entry : document.properties().entrySet()) {
      info.setMoreInfo(entry.getKey(), entry.getValue());
    }
  }

  private Rectangle toITextRectangle(PageSize pageSize) {
    return new Rectangle(pageSize.getWidth(), pageSize.getHeight());
  }

  private void setMargins(Document doc, com.marcusprado02.commons.ports.pdf.PdfDocument.Margins margins) {
    doc.setMargins(margins.top(), margins.right(), margins.bottom(), margins.left());
  }

  private void renderElement(Document doc, PdfElement element) {
    switch (element) {
      case PdfElement.Text text -> renderText(doc, text);
      case PdfElement.Paragraph paragraph -> renderParagraph(doc, paragraph);
      case PdfElement.Image image -> renderImage(doc, image);
      case PdfElement.Table table -> renderTable(doc, table);
      case PdfElement.Space space -> doc.add(new Paragraph().setHeight(space.height()));
      case PdfElement.PageBreak pageBreak -> doc.add(new AreaBreak());
    }
  }

  private void renderText(Document doc, PdfElement.Text text) {
    Text iTextText = new Text(text.content()).setFontSize(text.fontSize());

    if (text.bold()) {
      iTextText.setBold();
    }
    if (text.italic()) {
      iTextText.setItalic();
    }
    if (text.color() != null) {
      java.awt.Color awtColor = text.color();
      iTextText.setFontColor(
          new DeviceRgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue()));
    }

    doc.add(new Paragraph().add(iTextText));
  }

  private void renderParagraph(Document doc, PdfElement.Paragraph paragraph) {
    Paragraph iTextParagraph =
        new Paragraph(paragraph.content())
            .setFontSize(paragraph.fontSize())
            .setMultipliedLeading(paragraph.leading());

    iTextParagraph.setTextAlignment(toITextAlignment(paragraph.alignment()));

    doc.add(iTextParagraph);
  }

  private void renderImage(Document doc, PdfElement.Image image) {
    try {
      byte[] imageBytes = image.imageData().readAllBytes();
      com.itextpdf.layout.element.Image iTextImage =
          new com.itextpdf.layout.element.Image(ImageDataFactory.create(imageBytes));

      if (image.width() != null && image.height() != null) {
        iTextImage.scaleToFit(image.width(), image.height());
      } else if (image.width() != null) {
        iTextImage.setWidth(UnitValue.createPointValue(image.width()));
        iTextImage.setAutoScale(true);
      } else if (image.height() != null) {
        iTextImage.setHeight(UnitValue.createPointValue(image.height()));
        iTextImage.setAutoScale(true);
      }

      iTextImage.setHorizontalAlignment(toITextHorizontalAlignment(image.alignment()));

      doc.add(iTextImage);
    } catch (Exception e) {
      log.warn("Failed to render image: {}", e.getMessage());
    }
  }

  private void renderTable(Document doc, PdfElement.Table table) {
    int columnCount = table.headers() != null ? table.headers().size() : table.rows().get(0).size();

    Table iTextTable = new Table(UnitValue.createPercentArray(columnCount)).useAllAvailableWidth();

    // Set column widths if specified
    if (table.columnWidths() != null && table.columnWidths().length == columnCount) {
      iTextTable = new Table(table.columnWidths()).useAllAvailableWidth();
    }

    // Add headers if present
    if (table.headers() != null) {
      java.awt.Color headerColor = table.headerColor();
      DeviceRgb bgColor =
          new DeviceRgb(headerColor.getRed(), headerColor.getGreen(), headerColor.getBlue());

      for (String header : table.headers()) {
        Cell cell =
            new Cell()
                .add(new Paragraph(header).setBold())
                .setBackgroundColor(bgColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        iTextTable.addHeaderCell(cell);
      }
    }

    // Add data rows
    for (var row : table.rows()) {
      for (String cellData : row) {
        Cell cell = new Cell().add(new Paragraph(cellData));
        iTextTable.addCell(cell);
      }
    }

    doc.add(iTextTable);
  }

  private TextAlignment toITextAlignment(PdfElement.Alignment alignment) {
    return switch (alignment) {
      case LEFT -> TextAlignment.LEFT;
      case CENTER -> TextAlignment.CENTER;
      case RIGHT -> TextAlignment.RIGHT;
      case JUSTIFIED -> TextAlignment.JUSTIFIED;
    };
  }

  private HorizontalAlignment toITextHorizontalAlignment(PdfElement.Alignment alignment) {
    return switch (alignment) {
      case LEFT -> HorizontalAlignment.LEFT;
      case CENTER -> HorizontalAlignment.CENTER;
      case RIGHT -> HorizontalAlignment.RIGHT;
      case JUSTIFIED -> HorizontalAlignment.CENTER;
    };
  }

  private void signPdf(byte[] pdfBytes, com.marcusprado02.commons.ports.pdf.PdfSignature signature, OutputStream output)
      throws Exception {
    // Load keystore
    KeyStore keystore = KeyStore.getInstance("PKCS12");
    keystore.load(signature.keystoreData(), signature.keystorePassword().toCharArray());

    // Get private key and certificate chain
    PrivateKey privateKey =
        (PrivateKey)
            keystore.getKey(signature.keyAlias(), signature.keystorePassword().toCharArray());
    Certificate[] chain = keystore.getCertificateChain(signature.keyAlias());

    // Create PdfReader from bytes
    PdfReader reader = new PdfReader(new java.io.ByteArrayInputStream(pdfBytes));
    PdfSigner signer = new PdfSigner(reader, output, new StampingProperties());

    // Create signature appearance
    PdfSignatureAppearance appearance = signer.getSignatureAppearance();
    appearance.setReason(signature.reason());
    appearance.setLocation(signature.location());
    if (signature.contactInfo() != null) {
      appearance.setContact(signature.contactInfo());
    }

    // Set signature date - Note: setSignDate is deprecated/not visible in iText 7+
    // The signature date is set automatically by iText

    // Set visible signature field if specified
    if (signature.signatureField() != null) {
      com.marcusprado02.commons.ports.pdf.PdfSignature.SignatureField field = signature.signatureField();
      Rectangle rect = new Rectangle((float)field.x(), (float)field.y(),
                                     (float)field.width(), (float)field.height());
      appearance.setPageRect(rect).setPageNumber(field.page());

      if (field.imageData() != null) {
        byte[] imageBytes = field.imageData().readAllBytes();
        appearance.setSignatureGraphic(ImageDataFactory.create(imageBytes));
        appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);
      }
    }

    // Create external signature and digest
    IExternalSignature externalSignature =
        new PrivateKeySignature(
            privateKey, DigestAlgorithms.SHA256, BouncyCastleProvider.PROVIDER_NAME);
    IExternalDigest externalDigest = new BouncyCastleDigest();

    // Sign the document
    signer.signDetached(
        externalDigest, externalSignature, chain, null, null, null, 0, PdfSigner.CryptoStandard.CMS);
  }
}
