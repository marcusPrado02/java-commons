package com.marcusprado02.commons.ports.email;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EmailCompositionTest {

  private static final EmailAddress FROM = EmailAddress.of("sender@example.com");
  private static final EmailAddress RECIPIENT = EmailAddress.of("recipient@example.com");
  private static final EmailSubject SUBJECT = EmailSubject.of("Test Subject");

  // --- EmailContent ---

  @Test
  void emailContent_html_creates_html_only() {
    EmailContent c = EmailContent.html("<p>Hello</p>");
    assertTrue(c.hasHtml());
    assertFalse(c.hasText());
    assertEquals(Optional.of("<p>Hello</p>"), c.htmlContent());
  }

  @Test
  void emailContent_text_creates_text_only() {
    EmailContent c = EmailContent.text("Hello");
    assertFalse(c.hasHtml());
    assertTrue(c.hasText());
  }

  @Test
  void emailContent_both_creates_both() {
    EmailContent c = EmailContent.both("<p>Hi</p>", "Hi");
    assertTrue(c.hasHtml());
    assertTrue(c.hasText());
  }

  @Test
  void emailContent_null_html_content_throws() {
    assertThrows(NullPointerException.class,
        () -> new EmailContent(null, Optional.of("text")));
  }

  @Test
  void emailContent_null_text_content_throws() {
    assertThrows(NullPointerException.class,
        () -> new EmailContent(Optional.of("<p>html</p>"), null));
  }

  @Test
  void emailContent_both_empty_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> new EmailContent(Optional.empty(), Optional.empty()));
  }

  @Test
  void emailContent_blank_html_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> new EmailContent(Optional.of("   "), Optional.empty()));
  }

  @Test
  void emailContent_blank_text_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> new EmailContent(Optional.empty(), Optional.of("  ")));
  }

  // --- EmailAttachment ---

  @Test
  void emailAttachment_valid() {
    EmailAttachment a = EmailAttachment.of("file.pdf", "application/pdf", new byte[]{1, 2, 3});
    assertEquals("file.pdf", a.filename());
    assertEquals("application/pdf", a.contentType());
    assertEquals(3L, a.size());
  }

  @Test
  void emailAttachment_null_filename_throws() {
    assertThrows(NullPointerException.class,
        () -> new EmailAttachment(null, "application/pdf", new byte[]{1}));
  }

  @Test
  void emailAttachment_null_content_type_throws() {
    assertThrows(NullPointerException.class,
        () -> new EmailAttachment("file.pdf", null, new byte[]{1}));
  }

  @Test
  void emailAttachment_null_data_throws() {
    assertThrows(NullPointerException.class,
        () -> new EmailAttachment("file.pdf", "application/pdf", null));
  }

  @Test
  void emailAttachment_blank_filename_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> new EmailAttachment("  ", "application/pdf", new byte[]{1}));
  }

  @Test
  void emailAttachment_blank_content_type_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> new EmailAttachment("file.pdf", "  ", new byte[]{1}));
  }

  @Test
  void emailAttachment_empty_data_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> new EmailAttachment("file.pdf", "application/pdf", new byte[0]));
  }

  @Test
  void emailAttachment_fromStream_reads_bytes() {
    byte[] data = new byte[]{10, 20, 30};
    EmailAttachment a = EmailAttachment.fromStream("file.txt", "text/plain",
        new ByteArrayInputStream(data));
    assertEquals(3L, a.size());
    assertEquals("file.txt", a.filename());
  }

  // --- Email ---

  @Test
  void email_builder_valid() {
    EmailContent content = EmailContent.html("<p>Hello</p>");
    Email email = Email.builder()
        .from(FROM)
        .to(RECIPIENT)
        .subject(SUBJECT)
        .content(content)
        .build();
    assertEquals(FROM, email.from());
    assertEquals(1, email.to().size());
    assertFalse(email.hasAttachments());
    assertEquals(0L, email.totalAttachmentSize());
  }

  @Test
  void email_builder_with_string_addresses() {
    Email email = Email.builder()
        .from("sender@example.com")
        .to("recipient@example.com")
        .cc("cc@example.com")
        .bcc("bcc@example.com")
        .subject("Hello")
        .htmlContent("<p>Hi</p>")
        .build();
    assertEquals(1, email.cc().size());
    assertEquals(1, email.bcc().size());
  }

  @Test
  void email_builder_with_to_list() {
    Email email = Email.builder()
        .from(FROM)
        .to(List.of(RECIPIENT, EmailAddress.of("other@example.com")))
        .subject(SUBJECT)
        .textContent("Hello")
        .build();
    assertEquals(2, email.to().size());
  }

  @Test
  void email_builder_with_cc_bcc_lists() {
    Email email = Email.builder()
        .from(FROM)
        .to(RECIPIENT)
        .cc(List.of(EmailAddress.of("cc@example.com")))
        .bcc(List.of(EmailAddress.of("bcc@example.com")))
        .subject(SUBJECT)
        .bothContent("<p>Hi</p>", "Hi")
        .replyTo("reply@example.com")
        .build();
    assertEquals(1, email.cc().size());
    assertEquals(1, email.bcc().size());
    assertNotNull(email.replyTo());
  }

  @Test
  void email_empty_to_throws() {
    assertThrows(IllegalArgumentException.class, () ->
        new Email(FROM, List.of(), List.of(), List.of(), SUBJECT,
            EmailContent.html("<p>Hi</p>"), List.of(), null));
  }

  @Test
  void email_null_from_throws() {
    assertThrows(NullPointerException.class, () ->
        new Email(null, List.of(RECIPIENT), List.of(), List.of(), SUBJECT,
            EmailContent.html("<p>Hi</p>"), List.of(), null));
  }

  @Test
  void email_with_attachment_hasAttachments_true() {
    EmailAttachment att = EmailAttachment.of("f.pdf", "application/pdf", new byte[]{1, 2});
    Email email = Email.builder()
        .from(FROM)
        .to(RECIPIENT)
        .subject(SUBJECT)
        .htmlContent("<p>Hi</p>")
        .attachment(att)
        .build();
    assertTrue(email.hasAttachments());
    assertEquals(2L, email.totalAttachmentSize());
  }

  @Test
  void email_replyTo_phone_number_object() {
    Email email = Email.builder()
        .from(FROM)
        .to(RECIPIENT)
        .subject(SUBJECT)
        .htmlContent("<p>Hi</p>")
        .replyTo(EmailAddress.of("reply@example.com"))
        .attachments(List.of())
        .build();
    assertNotNull(email.replyTo());
  }

  // --- TemplateEmailRequest ---

  @Test
  void templateEmailRequest_builder_valid() {
    TemplateEmailRequest req = TemplateEmailRequest.builder()
        .from(FROM)
        .to(RECIPIENT)
        .subject(SUBJECT)
        .templateName("welcome")
        .variable("name", "Alice")
        .build();
    assertEquals("welcome", req.templateName());
    assertEquals(1, req.variables().size());
    assertTrue(req.attachments().isEmpty());
  }

  @Test
  void templateEmailRequest_builder_with_to_list() {
    TemplateEmailRequest req = TemplateEmailRequest.builder()
        .from(FROM)
        .to(List.of(RECIPIENT))
        .subject(SUBJECT)
        .templateName("tmpl")
        .build();
    assertEquals(1, req.to().size());
  }

  @Test
  void templateEmailRequest_builder_with_cc_bcc() {
    TemplateEmailRequest req = TemplateEmailRequest.builder()
        .from("sender@example.com")
        .to("r@example.com")
        .cc("cc@example.com")
        .bcc("bcc@example.com")
        .subject("Hello")
        .templateName("tmpl")
        .variables(Map.of("k", "v"))
        .attachment(EmailAttachment.of("f.pdf", "application/pdf", new byte[]{1}))
        .attachments(List.of())
        .replyTo(EmailAddress.of("reply@example.com"))
        .build();
    assertEquals(1, req.cc().size());
  }

  @Test
  void templateEmailRequest_empty_to_throws() {
    assertThrows(IllegalArgumentException.class, () ->
        new TemplateEmailRequest(FROM, List.of(), List.of(), List.of(), SUBJECT,
            "tmpl", Map.of(), List.of(), null));
  }

  @Test
  void templateEmailRequest_blank_template_name_throws() {
    assertThrows(IllegalArgumentException.class, () ->
        new TemplateEmailRequest(FROM, List.of(RECIPIENT), List.of(), List.of(), SUBJECT,
            "  ", Map.of(), List.of(), null));
  }
}
