package com.marcusprado02.commons.ports.template;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TemplateResultTest {

  @Test
  void templateResult_html_factory() {
    TemplateResult r = TemplateResult.html("welcome", "<p>Hello</p>");
    assertEquals("welcome", r.templateName());
    assertEquals("<p>Hello</p>", r.content());
    assertEquals("text/html", r.contentType());
    assertEquals(StandardCharsets.UTF_8, r.charset());
  }

  @Test
  void templateResult_text_factory() {
    TemplateResult r = TemplateResult.text("welcome", "Hello");
    assertEquals("text/plain", r.contentType());
  }

  @Test
  void templateResult_xml_factory() {
    TemplateResult r = TemplateResult.xml("report", "<root/>");
    assertEquals("application/xml", r.contentType());
  }

  @Test
  void templateResult_null_template_name_throws() {
    assertThrows(NullPointerException.class,
        () -> new TemplateResult(null, "content", "text/html", StandardCharsets.UTF_8));
  }

  @Test
  void templateResult_null_content_throws() {
    assertThrows(NullPointerException.class,
        () -> new TemplateResult("tmpl", null, "text/html", StandardCharsets.UTF_8));
  }

  @Test
  void templateResult_null_content_type_throws() {
    assertThrows(NullPointerException.class,
        () -> new TemplateResult("tmpl", "content", null, StandardCharsets.UTF_8));
  }

  @Test
  void templateResult_null_charset_throws() {
    assertThrows(NullPointerException.class,
        () -> new TemplateResult("tmpl", "content", "text/html", null));
  }

  @Test
  void templateResult_isEmpty_false_for_content() {
    TemplateResult r = TemplateResult.html("tmpl", "<p>Hi</p>");
    assertFalse(r.isEmpty());
  }

  @Test
  void templateResult_isEmpty_true_for_blank_content() {
    TemplateResult r = new TemplateResult("tmpl", "   ", "text/html", StandardCharsets.UTF_8);
    assertTrue(r.isEmpty());
  }

  @Test
  void templateResult_isEmpty_true_for_empty_string() {
    TemplateResult r = new TemplateResult("tmpl", "", "text/html", StandardCharsets.UTF_8);
    assertTrue(r.isEmpty());
  }

  @Test
  void templateResult_getContent_returns_content() {
    TemplateResult r = TemplateResult.text("tmpl", "Hello World");
    assertEquals("Hello World", r.getContent());
  }

  @Test
  void templateResult_getBytes_returns_encoded_bytes() {
    TemplateResult r = TemplateResult.text("tmpl", "Hello");
    byte[] bytes = r.getBytes();
    assertEquals("Hello", new String(bytes, StandardCharsets.UTF_8));
  }

  @Test
  void templateResult_getContentLength_matches_bytes() {
    TemplateResult r = TemplateResult.html("tmpl", "<p>Hi</p>");
    assertEquals(r.getBytes().length, r.getContentLength());
  }
}
