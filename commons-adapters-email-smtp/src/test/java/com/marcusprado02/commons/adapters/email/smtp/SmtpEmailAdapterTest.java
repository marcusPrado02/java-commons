package com.marcusprado02.commons.adapters.email.smtp;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.email.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SmtpEmailAdapterTest {

  @RegisterExtension
  static final GreenMailExtension greenMail =
      new GreenMailExtension(ServerSetupTest.SMTP)
          .withConfiguration(GreenMailConfiguration.aConfig().withUser("test", "test"))
          .withPerMethodLifecycle(true);

  private SmtpEmailAdapter emailAdapter;

  @BeforeEach
  void setUp() {
    SmtpConfiguration config =
        SmtpConfiguration.builder()
            .host("localhost")
            .port(3025)
            .requireAuth(false)
            .useTls(false)
            .useStartTls(false)
            .build();

    emailAdapter = new SmtpEmailAdapter(config);
  }

  @Test
  @DisplayName("Should send simple email successfully")
  void shouldSendEmail() {
    Email email =
        Email.builder()
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("Test Subject")
            .textContent("Test message")
            .build();

    Result<EmailPort.EmailReceipt> result = emailAdapter.send(email);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();
    assertThat(result.getOrNull().messageId()).isNotEmpty();
  }

  @Test
  @DisplayName("Should send HTML email successfully")
  void shouldSendHtmlEmail() {
    Email email =
        Email.builder()
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("HTML Email")
            .htmlContent("<h1>Hello</h1>")
            .build();

    Result<EmailPort.EmailReceipt> result = emailAdapter.send(email);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  @DisplayName("Should send email with both HTML and text content")
  void shouldSendEmailWithHtmlAndTextContent() {
    Email email =
        Email.builder()
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("Multipart Email")
            .textContent("Plain text")
            .htmlContent("<p>HTML content</p>")
            .build();

    Result<EmailPort.EmailReceipt> result = emailAdapter.send(email);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  @DisplayName("Should send email with CC, BCC and replyTo")
  void shouldSendEmailWithCcBccAndReplyTo() {
    Email email =
        Email.builder()
            .from("sender@example.com")
            .to("recipient@example.com")
            .cc("cc@example.com")
            .bcc("bcc@example.com")
            .replyTo("reply@example.com")
            .subject("CC BCC Test")
            .textContent("Content")
            .build();

    Result<EmailPort.EmailReceipt> result = emailAdapter.send(email);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  @DisplayName("Should send email with attachment")
  void shouldSendEmailWithAttachment() {
    Email email =
        Email.builder()
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("Email with Attachment")
            .textContent("See attached")
            .attachment(
                EmailAttachment.of(
                    "file.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8)))
            .build();

    Result<EmailPort.EmailReceipt> result = emailAdapter.send(email);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  @DisplayName("Should send with requireAuth=true using correct credentials")
  void shouldSendWithAuthentication() {
    SmtpConfiguration authConfig =
        SmtpConfiguration.builder()
            .host("localhost")
            .port(3025)
            .username("test")
            .password("test")
            .requireAuth(true)
            .useTls(false)
            .useStartTls(false)
            .build();

    SmtpEmailAdapter authAdapter = new SmtpEmailAdapter(authConfig);

    Email email =
        Email.builder()
            .from("test@example.com")
            .to("recipient@example.com")
            .subject("Auth Test")
            .textContent("Hello")
            .build();

    // GreenMail accepts any send regardless of auth in SMTP mode
    Result<EmailPort.EmailReceipt> result = authAdapter.send(email);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  @DisplayName("Should fail template send without renderer")
  void shouldFailTemplateSendWithoutRenderer() {
    TemplateEmailRequest templateRequest =
        TemplateEmailRequest.builder()
            .templateName("welcome")
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("Welcome")
            .variable("name", "John")
            .build();

    Result<EmailPort.EmailReceipt> result = emailAdapter.sendWithTemplate(templateRequest);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull()).isNotNull();
    assertThat(result.problemOrNull().code().value()).contains("TEMPLATE");
  }

  @Test
  @DisplayName("Should succeed with template renderer that renders successfully")
  void shouldSendWithTemplateRenderer() {
    TemplateRenderer mockRenderer = (templateName, variables) -> "<h1>Hello</h1>";
    SmtpEmailAdapter adapterWithRenderer =
        new SmtpEmailAdapter(
            SmtpConfiguration.builder().host("localhost").port(3025).requireAuth(false).build(),
            mockRenderer);

    TemplateEmailRequest templateRequest =
        TemplateEmailRequest.builder()
            .templateName("welcome")
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("Welcome")
            .variable("name", "John")
            .build();

    Result<EmailPort.EmailReceipt> result = adapterWithRenderer.sendWithTemplate(templateRequest);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  @DisplayName("Should return fail when template renderer throws exception")
  void shouldReturnFailWhenTemplateRendererThrows() {
    TemplateRenderer failingRenderer =
        (templateName, variables) -> {
          throw new RuntimeException("Template not found");
        };
    SmtpEmailAdapter adapterWithRenderer =
        new SmtpEmailAdapter(
            SmtpConfiguration.builder().host("localhost").port(3025).requireAuth(false).build(),
            failingRenderer);

    TemplateEmailRequest templateRequest =
        TemplateEmailRequest.builder()
            .templateName("missing")
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("Test")
            .build();

    Result<EmailPort.EmailReceipt> result = adapterWithRenderer.sendWithTemplate(templateRequest);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).contains("Template not found");
  }

  @Test
  @DisplayName("Should verify connection successfully")
  void shouldVerifyConnection() {
    Result<Void> result = emailAdapter.verify();

    assertThat(result.isOk()).isTrue();
  }

  @Test
  @DisplayName("Should return fail when verify connection fails")
  void shouldReturnFailWhenVerifyConnectionFails() {
    SmtpConfiguration wrongConfig =
        SmtpConfiguration.builder()
            .host("localhost")
            .port(19999) // no server on this port
            .requireAuth(false)
            .build();
    SmtpEmailAdapter failingAdapter = new SmtpEmailAdapter(wrongConfig);

    Result<Void> result = failingAdapter.verify();

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().message()).isNotNull();
  }
}
