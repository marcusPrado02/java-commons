package com.marcusprado02.commons.ports.pdf;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PdfDocumentTest {

  @Test
  void pdfDocument_builder_creates_document_with_defaults() {
    PdfDocument doc = PdfDocument.builder()
        .title("Test Doc")
        .author("Alice")
        .build();

    assertEquals("Test Doc", doc.title());
    assertEquals("Alice", doc.author());
    assertEquals(PageSize.A4, doc.pageSize());
    assertNotNull(doc.margins());
    assertNotNull(doc.createdAt());
    assertTrue(doc.elements().isEmpty());
    assertTrue(doc.properties().isEmpty());
  }

  @Test
  void pdfDocument_null_createdAt_defaults_to_now() {
    PdfDocument doc = PdfDocument.builder().build();
    assertNotNull(doc.createdAt());
  }

  @Test
  void pdfDocument_null_properties_defaults_to_empty_map() {
    PdfDocument doc = new PdfDocument(null, null, null, null, null,
        PageSize.A4, PdfDocument.Margins.standard(), List.of(), null, null);
    assertNotNull(doc.properties());
    assertTrue(doc.properties().isEmpty());
  }

  @Test
  void pdfDocument_builder_property_method() {
    PdfDocument doc = PdfDocument.builder()
        .property("author", "Bob")
        .property("version", "1.0")
        .build();
    assertEquals("Bob", doc.properties().get("author"));
    assertEquals("1.0", doc.properties().get("version"));
  }

  @Test
  void pdfDocument_builder_with_existing_properties_map() {
    PdfDocument doc = PdfDocument.builder()
        .properties(Map.of("k", "v"))
        .property("k2", "v2")
        .build();
    assertEquals("v2", doc.properties().get("k2"));
  }

  @Test
  void pdfDocument_toBuilder_preserves_fields() {
    Instant ts = Instant.parse("2026-01-01T00:00:00Z");
    PdfDocument original = PdfDocument.builder()
        .title("Original").author("A").createdAt(ts).build();
    PdfDocument copy = original.toBuilder().title("Copy").build();
    assertEquals("Copy", copy.title());
    assertEquals("A", copy.author());
    assertEquals(ts, copy.createdAt());
  }

  @Test
  void pdfDocument_margins_uniform() {
    PdfDocument.Margins m = PdfDocument.Margins.uniform(36f);
    assertEquals(36f, m.top());
    assertEquals(36f, m.right());
    assertEquals(36f, m.bottom());
    assertEquals(36f, m.left());
  }

  @Test
  void pdfDocument_margins_standard_is_72_points() {
    PdfDocument.Margins m = PdfDocument.Margins.standard();
    assertEquals(72f, m.top());
  }

  @Test
  void pdfDocument_margins_narrow_is_36_points() {
    PdfDocument.Margins m = PdfDocument.Margins.narrow();
    assertEquals(36f, m.top());
  }

  @Test
  void pdfDocument_builder_margins_float_overload() {
    PdfDocument doc = PdfDocument.builder().margins(50f).build();
    assertEquals(50f, doc.margins().top());
  }

  @Test
  void pdfDocument_subject_keywords_creator() {
    PdfDocument doc = PdfDocument.builder()
        .subject("subj").keywords("a,b").creator("App").build();
    assertEquals("subj", doc.subject());
    assertEquals("a,b", doc.keywords());
    assertEquals("App", doc.creator());
  }
}
