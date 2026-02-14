package com.marcusprado02.commons.adapters.email.smtp;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.email.*;
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
    // Given
    Email email =
        Email.builder()
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("Test Subject")
            .textContent("Test message")
            .build();

    // When
    Result<EmailPort.EmailReceipt> result = emailAdapter.send(email);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isNotNull();
    assertThat(result.getOrNull().messageId()).isNotEmpty();
  }

  @Test
  @DisplayName("Should fail template send without renderer")
  void shouldFailTemplateSendWithoutRenderer() {
    // Given
    TemplateEmailRequest templateRequest =
        TemplateEmailRequest.builder()
            .templateName("welcome")
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("Welcome")
            .variable("name", "John")
            .build();

    // When
    Result<EmailPort.EmailReceipt> result = emailAdapter.sendWithTemplate(templateRequest);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull()).isNotNull();
    assertThat(result.problemOrNull().code().value()).contains("TEMPLATE");
  }

  @Test
  @DisplayName("Should verify connection successfully")
  void shouldVerifyConnection() {
    // When
    Result<Void> result = emailAdapter.verify();

    // Then
    assertThat(result.isOk()).isTrue();
  }
}
