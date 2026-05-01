package com.marcusprado02.commons.ports.pdf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PdfModelExtendedTest {

  private static final InputStream KEYSTORE = new ByteArrayInputStream(new byte[] {1, 2, 3});

  // --- PdfElement.Text ---

  @Test
  void text_single_arg_constructor() {
    PdfElement.Text t = new PdfElement.Text("Hello");
    assertEquals("Hello", t.content());
    assertEquals(12f, t.fontSize());
    assertFalse(t.bold());
    assertFalse(t.italic());
    assertNull(t.color());
  }

  @Test
  void text_two_arg_constructor() {
    PdfElement.Text t = new PdfElement.Text("Hi", 18f);
    assertEquals(18f, t.fontSize());
    assertFalse(t.bold());
  }

  @Test
  void text_full_constructor() {
    PdfElement.Text t = new PdfElement.Text("Hi", 14f, true, true, java.awt.Color.RED);
    assertTrue(t.bold());
    assertTrue(t.italic());
    assertEquals(java.awt.Color.RED, t.color());
  }

  // --- PdfElement.Paragraph ---

  @Test
  void paragraph_single_arg() {
    PdfElement.Paragraph p = new PdfElement.Paragraph("text");
    assertEquals("text", p.content());
    assertEquals(PdfElement.Alignment.LEFT, p.alignment());
  }

  @Test
  void paragraph_two_arg() {
    PdfElement.Paragraph p = new PdfElement.Paragraph("text", PdfElement.Alignment.CENTER);
    assertEquals(PdfElement.Alignment.CENTER, p.alignment());
  }

  @Test
  void paragraph_full_constructor() {
    PdfElement.Paragraph p =
        new PdfElement.Paragraph("text", 16f, PdfElement.Alignment.RIGHT, 2.0f);
    assertEquals(16f, p.fontSize());
    assertEquals(2.0f, p.leading());
  }

  // --- PdfElement.Image ---

  @Test
  void image_single_arg() {
    InputStream is = new ByteArrayInputStream(new byte[0]);
    PdfElement.Image img = new PdfElement.Image(is);
    assertSame(is, img.imageData());
    assertNull(img.width());
    assertNull(img.height());
    assertEquals(PdfElement.Alignment.LEFT, img.alignment());
  }

  @Test
  void image_three_arg() {
    InputStream is = new ByteArrayInputStream(new byte[0]);
    PdfElement.Image img = new PdfElement.Image(is, 100f, 200f);
    assertEquals(100f, img.width());
    assertEquals(200f, img.height());
    assertEquals(PdfElement.Alignment.CENTER, img.alignment());
  }

  // --- PdfElement.Table ---

  @Test
  void table_rows_only() {
    PdfElement.Table t = new PdfElement.Table(List.of(List.of("a", "b")));
    assertNull(t.headers());
    assertEquals(1, t.rows().size());
    assertNotNull(t.headerColor());
  }

  @Test
  void table_with_headers() {
    PdfElement.Table t = new PdfElement.Table(List.of("Col1", "Col2"), List.of(List.of("a", "b")));
    assertEquals(2, t.headers().size());
  }

  // --- PdfElement.Space ---

  @Test
  void space_default_constructor() {
    PdfElement.Space s = new PdfElement.Space();
    assertEquals(12f, s.height());
  }

  @Test
  void space_custom_height() {
    PdfElement.Space s = new PdfElement.Space(24f);
    assertEquals(24f, s.height());
  }

  // --- PdfElement.PageBreak ---

  @Test
  void pageBreak_is_pdf_element() {
    PdfElement pb = new PdfElement.PageBreak();
    assertNotNull(pb);
  }

  // --- PdfElement.Alignment ---

  @Test
  void alignment_all_values() {
    assertEquals(4, PdfElement.Alignment.values().length);
    assertNotNull(PdfElement.Alignment.JUSTIFIED);
  }

  // --- PdfSignature ---

  @Test
  void pdfSignature_builder_valid() {
    InputStream ks = new ByteArrayInputStream(new byte[] {1});
    PdfSignature sig =
        PdfSignature.builder()
            .keystoreData(ks)
            .keystorePassword("pass")
            .keyAlias("alias")
            .reason("testing")
            .location("City")
            .contactInfo("user@example.com")
            .timestamp(Instant.now())
            .build();
    assertNotNull(sig);
    assertEquals("testing", sig.reason());
    assertEquals("City", sig.location());
  }

  @Test
  void pdfSignature_null_keystore_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PdfSignature.builder()
                .keystoreData(null)
                .keystorePassword("pass")
                .keyAlias("alias")
                .build());
  }

  @Test
  void pdfSignature_blank_password_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PdfSignature.builder()
                .keystoreData(new ByteArrayInputStream(new byte[1]))
                .keystorePassword("  ")
                .keyAlias("alias")
                .build());
  }

  @Test
  void pdfSignature_null_password_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PdfSignature.builder()
                .keystoreData(new ByteArrayInputStream(new byte[1]))
                .keystorePassword(null)
                .keyAlias("alias")
                .build());
  }

  @Test
  void pdfSignature_blank_alias_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PdfSignature.builder()
                .keystoreData(new ByteArrayInputStream(new byte[1]))
                .keystorePassword("pass")
                .keyAlias("  ")
                .build());
  }

  @Test
  void pdfSignature_null_timestamp_defaults_to_now() {
    Instant before = Instant.now();
    PdfSignature sig =
        PdfSignature.builder()
            .keystoreData(new ByteArrayInputStream(new byte[1]))
            .keystorePassword("pass")
            .keyAlias("alias")
            .build();
    assertFalse(sig.timestamp().isBefore(before));
  }

  @Test
  void pdfSignature_builder_with_signature_field_coords() {
    PdfSignature sig =
        PdfSignature.builder()
            .keystoreData(new ByteArrayInputStream(new byte[1]))
            .keystorePassword("pass")
            .keyAlias("alias")
            .signatureField(1, 10f, 20f, 100f, 50f)
            .build();
    assertNotNull(sig.signatureField());
    assertEquals(1, sig.signatureField().page());
  }

  @Test
  void pdfSignature_builder_with_signature_field_object() {
    PdfSignature.SignatureField field = new PdfSignature.SignatureField(1, 0f, 0f, 100f, 50f);
    PdfSignature sig =
        PdfSignature.builder()
            .keystoreData(new ByteArrayInputStream(new byte[1]))
            .keystorePassword("pass")
            .keyAlias("alias")
            .signatureField(field)
            .build();
    assertNotNull(sig.signatureField());
  }

  // --- PdfSignature.SignatureField ---

  @Test
  void signatureField_valid() {
    PdfSignature.SignatureField f = new PdfSignature.SignatureField(1, 10f, 20f, 100f, 50f);
    assertEquals(1, f.page());
    assertNull(f.imageData());
  }

  @Test
  void signatureField_page_zero_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PdfSignature.SignatureField(0, 0f, 0f, 100f, 50f));
  }

  @Test
  void signatureField_zero_width_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> new PdfSignature.SignatureField(1, 0f, 0f, 0f, 50f));
  }

  @Test
  void signatureField_with_image_data() {
    InputStream img = new ByteArrayInputStream(new byte[] {1, 2});
    PdfSignature.SignatureField f = new PdfSignature.SignatureField(1, 0f, 0f, 100f, 50f, img);
    assertNotNull(f.imageData());
  }
}
