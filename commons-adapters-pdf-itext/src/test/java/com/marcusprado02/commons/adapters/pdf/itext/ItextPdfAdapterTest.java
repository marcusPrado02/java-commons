package com.marcusprado02.commons.adapters.pdf.itext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.pdf.PdfDocument;
import com.marcusprado02.commons.ports.pdf.PdfElement;
import com.marcusprado02.commons.ports.pdf.PdfSignature;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

class ItextPdfAdapterTest {

  // ── ItextConfiguration factory and builder ───────────────────────────────

  @Test
  void defaultConfigShouldHaveExpectedDefaults() {
    var config = ItextConfiguration.defaultConfig();
    assertThat(config.compressContent()).isTrue();
    assertThat(config.fullCompressionMode()).isFalse();
    assertThat(config.pdfVersion()).isEqualTo("1.7");
    assertThat(config.encryptionBits()).isEqualTo(0);
    assertThat(config.bufferSize()).isEqualTo(8192);
    assertThat(config.userPassword()).isNull();
    assertThat(config.ownerPassword()).isNull();
  }

  @Test
  void compressedConfigShouldHaveBothCompressionFlags() {
    var config = ItextConfiguration.compressed();
    assertThat(config.compressContent()).isTrue();
    assertThat(config.fullCompressionMode()).isTrue();
  }

  @Test
  void pdf20ConfigShouldHavePdf20Version() {
    var config = ItextConfiguration.pdf20();
    assertThat(config.pdfVersion()).isEqualTo("2.0");
    assertThat(config.compressContent()).isTrue();
  }

  @Test
  void encryptedConfigShouldHavePasswordsAndEncryptionBits() {
    var config = ItextConfiguration.encrypted("user123", "owner456");
    assertThat(config.userPassword()).isEqualTo("user123");
    assertThat(config.ownerPassword()).isEqualTo("owner456");
    assertThat(config.encryptionBits()).isEqualTo(256);
  }

  @Test
  void builderShouldSetAllFields() {
    var config =
        ItextConfiguration.builder()
            .compressContent(false)
            .fullCompressionMode(true)
            .pdfVersion("1.4")
            .userPassword("u")
            .ownerPassword("o")
            .encryptionBits(40)
            .bufferSize(4096)
            .build();

    assertThat(config.compressContent()).isFalse();
    assertThat(config.fullCompressionMode()).isTrue();
    assertThat(config.pdfVersion()).isEqualTo("1.4");
    assertThat(config.userPassword()).isEqualTo("u");
    assertThat(config.ownerPassword()).isEqualTo("o");
    assertThat(config.encryptionBits()).isEqualTo(40);
    assertThat(config.bufferSize()).isEqualTo(4096);
  }

  @Test
  void invalidPdfVersionShouldThrow() {
    assertThatThrownBy(() -> ItextConfiguration.builder().pdfVersion("1.3").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid PDF version");
  }

  @Test
  void invalidEncryptionBitsShouldThrow() {
    assertThatThrownBy(() -> ItextConfiguration.builder().encryptionBits(64).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Encryption bits");
  }

  @Test
  void nonPositiveBufferSizeShouldThrow() {
    assertThatThrownBy(() -> ItextConfiguration.builder().bufferSize(0).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Buffer size");
  }

  // ── supportsSignatures / getPdfVersion ───────────────────────────────────

  @Test
  void supportsSignaturesShouldReturnTrue() {
    assertThat(new ItextPdfAdapter().supportsSignatures()).isTrue();
  }

  @Test
  void getPdfVersionShouldReturnFromConfig() {
    var adapter = new ItextPdfAdapter(ItextConfiguration.builder().pdfVersion("2.0").build());
    assertThat(adapter.getPdfVersion()).isEqualTo("2.0");
  }

  // ── generate: element types ───────────────────────────────────────────────

  @Test
  void generateEmptyDocumentShouldSucceed() {
    var adapter = new ItextPdfAdapter();
    var doc = PdfDocument.builder().title("Empty").build();
    var out = new ByteArrayOutputStream();

    Result<Void> result = adapter.generate(doc, out);

    assertThat(result.isOk()).isTrue();
    assertThat(out.toByteArray()).hasSizeGreaterThan(0);
  }

  @Test
  void generateWithPlainTextShouldSucceed() {
    var adapter = new ItextPdfAdapter();
    var doc =
        PdfDocument.builder()
            .title("Text Test")
            .element(new PdfElement.Text("Hello World"))
            .element(new PdfElement.Text("Sized", 16f))
            .build();

    Result<Void> result = adapter.generate(doc, new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithBoldItalicColoredTextShouldSucceed() {
    var adapter = new ItextPdfAdapter();
    var coloredText = new PdfElement.Text("Styled", 14f, true, true, Color.BLUE);
    var doc = PdfDocument.builder().element(coloredText).build();

    Result<Void> result = adapter.generate(doc, new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithParagraphAllAlignmentsShouldSucceed() {
    var adapter = new ItextPdfAdapter();
    var doc =
        PdfDocument.builder()
            .element(new PdfElement.Paragraph("Left", PdfElement.Alignment.LEFT))
            .element(new PdfElement.Paragraph("Center", PdfElement.Alignment.CENTER))
            .element(new PdfElement.Paragraph("Right", PdfElement.Alignment.RIGHT))
            .element(new PdfElement.Paragraph("Justified", PdfElement.Alignment.JUSTIFIED))
            .build();

    Result<Void> result = adapter.generate(doc, new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithTableWithoutHeadersShouldSucceed() {
    var adapter = new ItextPdfAdapter();
    var rows = List.of(List.of("A1", "B1"), List.of("A2", "B2"));
    var doc = PdfDocument.builder().element(new PdfElement.Table(rows)).build();

    Result<Void> result = adapter.generate(doc, new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithTableWithHeadersShouldSucceed() {
    var adapter = new ItextPdfAdapter();
    var headers = List.of("Name", "Value");
    var rows = List.of(List.of("Alice", "42"), List.of("Bob", "17"));
    var doc = PdfDocument.builder().element(new PdfElement.Table(headers, rows)).build();

    Result<Void> result = adapter.generate(doc, new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithTableCustomColumnWidthsShouldSucceed() {
    var adapter = new ItextPdfAdapter();
    var headers = List.of("Col1", "Col2");
    var rows = List.of(List.of("X", "Y"));
    var element = new PdfElement.Table(headers, rows, new float[] {2f, 1f}, Color.LIGHT_GRAY);
    var doc = PdfDocument.builder().element(element).build();

    Result<Void> result = adapter.generate(doc, new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithSpaceAndPageBreakShouldSucceed() {
    var adapter = new ItextPdfAdapter();
    var doc =
        PdfDocument.builder()
            .element(new PdfElement.Text("Page 1"))
            .element(new PdfElement.Space(24f))
            .element(new PdfElement.PageBreak())
            .element(new PdfElement.Text("Page 2"))
            .element(new PdfElement.Space()) // default height
            .build();

    Result<Void> result = adapter.generate(doc, new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithImageShouldSucceedOrLogWarning() throws Exception {
    // Create a tiny PNG image in-memory
    BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
    ImageIO.write(img, "PNG", imgBaos);
    byte[] pngBytes = imgBaos.toByteArray();

    var adapter = new ItextPdfAdapter();
    // Image with no width/height
    var imgNoSize = new PdfElement.Image(new ByteArrayInputStream(pngBytes));
    // Image with both width and height
    var imgBothSize = new PdfElement.Image(new ByteArrayInputStream(pngBytes), 100f, 100f);

    var doc = PdfDocument.builder().element(imgNoSize).element(imgBothSize).build();

    // renderImage swallows exceptions; result should still be ok
    Result<Void> result = adapter.generate(doc, new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithImageWidthOnlyShouldSucceedOrLogWarning() throws Exception {
    BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
    ImageIO.write(img, "PNG", imgBaos);

    var adapter = new ItextPdfAdapter();
    var imgWidthOnly =
        new PdfElement.Image(
            new ByteArrayInputStream(imgBaos.toByteArray()), 50f, null, PdfElement.Alignment.LEFT);

    var doc = PdfDocument.builder().element(imgWidthOnly).build();
    Result<Void> result = adapter.generate(doc, new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithImageHeightOnlyShouldSucceedOrLogWarning() throws Exception {
    BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
    ImageIO.write(img, "PNG", imgBaos);

    var adapter = new ItextPdfAdapter();
    var imgHeightOnly =
        new PdfElement.Image(
            new ByteArrayInputStream(imgBaos.toByteArray()), null, 50f, PdfElement.Alignment.RIGHT);

    var doc = PdfDocument.builder().element(imgHeightOnly).build();
    Result<Void> result = adapter.generate(doc, new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  // ── generate: all PDF versions ────────────────────────────────────────────

  @Test
  void generateWithPdf14ShouldSucceed() {
    var adapter = new ItextPdfAdapter(ItextConfiguration.builder().pdfVersion("1.4").build());
    Result<Void> result = adapter.generate(minimalDoc(), new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithPdf15ShouldSucceed() {
    var adapter = new ItextPdfAdapter(ItextConfiguration.builder().pdfVersion("1.5").build());
    Result<Void> result = adapter.generate(minimalDoc(), new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithPdf16ShouldSucceed() {
    var adapter = new ItextPdfAdapter(ItextConfiguration.builder().pdfVersion("1.6").build());
    Result<Void> result = adapter.generate(minimalDoc(), new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithPdf17ShouldSucceed() {
    Result<Void> result = new ItextPdfAdapter().generate(minimalDoc(), new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithPdf20ShouldSucceed() {
    var adapter = new ItextPdfAdapter(ItextConfiguration.pdf20());
    Result<Void> result = adapter.generate(minimalDoc(), new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  // ── generate: compression and encryption variants ────────────────────────

  @Test
  void generateWithFullCompressionShouldSucceed() {
    var adapter = new ItextPdfAdapter(ItextConfiguration.compressed());
    Result<Void> result = adapter.generate(minimalDoc(), new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithEncryption256ShouldSucceed() {
    var config = ItextConfiguration.encrypted("pass", "ownerpass");
    var adapter = new ItextPdfAdapter(config);
    Result<Void> result = adapter.generate(minimalDoc(), new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithEncryption40BitShouldSucceed() {
    var config =
        ItextConfiguration.builder()
            .userPassword("u")
            .ownerPassword("o")
            .encryptionBits(40)
            .build();
    var adapter = new ItextPdfAdapter(config);
    Result<Void> result = adapter.generate(minimalDoc(), new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithEncryption128BitShouldSucceed() {
    var config =
        ItextConfiguration.builder()
            .userPassword("u")
            .ownerPassword("o")
            .encryptionBits(128)
            .build();
    var adapter = new ItextPdfAdapter(config);
    Result<Void> result = adapter.generate(minimalDoc(), new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithOwnerPasswordOnlyShouldSucceed() {
    // Only ownerPassword set (userPassword null), exercises null-check branches in
    // createWriterProperties
    var config =
        ItextConfiguration.builder().ownerPassword("owneronly").encryptionBits(128).build();
    var adapter = new ItextPdfAdapter(config);
    Result<Void> result = adapter.generate(minimalDoc(), new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void generateWithDocumentMetadataShouldSucceed() {
    var adapter = new ItextPdfAdapter();
    var doc =
        PdfDocument.builder()
            .title("My Title")
            .author("Jane Doe")
            .subject("Testing")
            .keywords("java,pdf,test")
            .creator("TestKit")
            .property("env", "ci")
            .property("version", "0.1.0")
            .element(new PdfElement.Text("Content"))
            .build();

    Result<Void> result = adapter.generate(doc, new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  // ── generateAndSign ───────────────────────────────────────────────────────

  @Test
  void generateAndSignWithValidKeystoreShouldSucceed() throws Exception {
    // Build a self-signed PKCS12 keystore entirely in-memory using BouncyCastle
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048, new SecureRandom());
    KeyPair keyPair = kpg.generateKeyPair();

    X500Name subject = new X500Name("CN=Test,O=Test,C=BR");
    Date now = new Date();
    Date expiry = new Date(now.getTime() + 365L * 24 * 3600 * 1000);

    ContentSigner signer =
        new JcaContentSignerBuilder("SHA256withRSA")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keyPair.getPrivate());

    var certHolder =
        new JcaX509v3CertificateBuilder(
                subject, BigInteger.ONE, now, expiry, subject, keyPair.getPublic())
            .build(signer);

    X509Certificate cert =
        new JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(certHolder);

    String password = "testpass";
    String alias = "testkey";
    KeyStore ks = KeyStore.getInstance("PKCS12");
    ks.load(null, null);
    ks.setKeyEntry(alias, keyPair.getPrivate(), password.toCharArray(), new Certificate[] {cert});

    ByteArrayOutputStream ksBytes = new ByteArrayOutputStream();
    ks.store(ksBytes, password.toCharArray());

    InputStream keystoreStream = new ByteArrayInputStream(ksBytes.toByteArray());
    PdfSignature signature =
        PdfSignature.builder()
            .keystoreData(keystoreStream)
            .keystorePassword(password)
            .keyAlias(alias)
            .reason("Testing")
            .location("CI")
            .contactInfo("test@example.com")
            .build();

    var adapter = new ItextPdfAdapter();
    var doc =
        PdfDocument.builder().title("Signed Doc").element(new PdfElement.Text("Signed")).build();
    var out = new ByteArrayOutputStream();

    Result<Void> result = adapter.generateAndSign(doc, signature, out);
    assertThat(result.isOk()).isTrue();
    assertThat(out.toByteArray()).hasSizeGreaterThan(0);
  }

  @Test
  void generateAndSignWithVisibleSignatureFieldShouldSucceed() throws Exception {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048, new SecureRandom());
    KeyPair keyPair = kpg.generateKeyPair();

    X500Name subject = new X500Name("CN=VisibleSig,C=BR");
    Date now = new Date();
    Date expiry = new Date(now.getTime() + 365L * 24 * 3600 * 1000);
    ContentSigner signer =
        new JcaContentSignerBuilder("SHA256withRSA")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keyPair.getPrivate());
    var certHolder =
        new JcaX509v3CertificateBuilder(
                subject, BigInteger.TWO, now, expiry, subject, keyPair.getPublic())
            .build(signer);
    X509Certificate cert =
        new JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(certHolder);

    String password = "vispass";
    String alias = "viskey";
    KeyStore ks = KeyStore.getInstance("PKCS12");
    ks.load(null, null);
    ks.setKeyEntry(
        alias,
        keyPair.getPrivate(),
        password.toCharArray(),
        new java.security.cert.Certificate[] {cert});

    ByteArrayOutputStream ksBytes = new ByteArrayOutputStream();
    ks.store(ksBytes, password.toCharArray());

    PdfSignature signature =
        PdfSignature.builder()
            .keystoreData(new ByteArrayInputStream(ksBytes.toByteArray()))
            .keystorePassword(password)
            .keyAlias(alias)
            .reason("Visible sig")
            .location("Test")
            .signatureField(1, 50f, 50f, 200f, 50f) // visible field on page 1
            .build();

    var adapter = new ItextPdfAdapter();
    var doc =
        PdfDocument.builder().title("Visible Sig Doc").element(new PdfElement.Text("Body")).build();

    Result<Void> result = adapter.generateAndSign(doc, signature, new ByteArrayOutputStream());
    assertThat(result.isOk()).isTrue();
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private PdfDocument minimalDoc() {
    return PdfDocument.builder().title("Test").element(new PdfElement.Text("Minimal")).build();
  }
}
