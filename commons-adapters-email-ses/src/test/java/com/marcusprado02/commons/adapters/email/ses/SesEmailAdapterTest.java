package com.marcusprado02.commons.adapters.email.ses;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.ports.email.Email;
import com.marcusprado02.commons.ports.email.EmailPort;
import com.marcusprado02.commons.ports.email.TemplateEmailRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

@ExtendWith(MockitoExtension.class)
class SesEmailAdapterTest {

  @Mock private SesV2Client sesClient;

  private SesEmailAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = SesEmailAdapter.create(sesClient);
  }

  @Test
  void shouldSendSimpleEmailSuccessfully() {
    when(sesClient.sendEmail(any(SendEmailRequest.class)))
        .thenReturn(SendEmailResponse.builder().messageId("msg-123").build());

    var email =
        Email.builder()
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("Hello")
            .textContent("Hello world")
            .build();

    var result = adapter.send(email);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().messageId()).isEqualTo("msg-123");
    verify(sesClient).sendEmail(any(SendEmailRequest.class));
  }

  @Test
  void shouldSendHtmlEmailSuccessfully() {
    when(sesClient.sendEmail(any(SendEmailRequest.class)))
        .thenReturn(SendEmailResponse.builder().messageId("msg-html-456").build());

    var email =
        Email.builder()
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("HTML Email")
            .htmlContent("<h1>Hello!</h1>")
            .build();

    var result = adapter.send(email);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().messageId()).isEqualTo("msg-html-456");
  }

  @Test
  void shouldReturnFailOnNullEmail() {
    var result = adapter.send(null);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("SES.NULL_EMAIL");
    verifyNoInteractions(sesClient);
  }

  @Test
  void shouldMapSesUnauthorizedToFailure() {
    var errorDetails = AwsErrorDetails.builder().errorMessage("Not authorized").build();
    when(sesClient.sendEmail(any(SendEmailRequest.class)))
        .thenThrow(
            (SesV2Exception)
                SesV2Exception.builder().awsErrorDetails(errorDetails).statusCode(403).build());

    var email =
        Email.builder()
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("Test")
            .textContent("body")
            .build();

    var result = adapter.send(email);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("SES.UNAUTHORIZED");
  }

  @Test
  void shouldMapSesRateLimitToFailure() {
    var errorDetails = AwsErrorDetails.builder().errorMessage("Rate exceeded").build();
    when(sesClient.sendEmail(any(SendEmailRequest.class)))
        .thenThrow(
            (SesV2Exception)
                SesV2Exception.builder().awsErrorDetails(errorDetails).statusCode(429).build());

    var email =
        Email.builder()
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("Test")
            .textContent("body")
            .build();

    var result = adapter.send(email);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("SES.RATE_LIMITED");
  }

  @Test
  void shouldSendTemplateEmailSuccessfully() {
    when(sesClient.sendEmail(any(SendEmailRequest.class)))
        .thenReturn(SendEmailResponse.builder().messageId("tmpl-789").build());

    var templateRequest =
        TemplateEmailRequest.builder()
            .from("sender@example.com")
            .to("recipient@example.com")
            .subject("Welcome")
            .templateName("WelcomeTemplate")
            .variable("name", "John")
            .variable("activationUrl", "https://example.com/activate")
            .build();

    var result = adapter.sendWithTemplate(templateRequest);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().messageId()).isEqualTo("tmpl-789");
  }

  @Test
  void shouldReturnFailOnNullTemplateRequest() {
    var result = adapter.sendWithTemplate(null);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("SES.NULL_TEMPLATE_REQUEST");
    verifyNoInteractions(sesClient);
  }

  @Test
  void shouldVerifyConnectionSuccessfully() {
    when(sesClient.listEmailIdentities(any(ListEmailIdentitiesRequest.class)))
        .thenReturn(ListEmailIdentitiesResponse.builder().build());

    var result = adapter.verify();

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void shouldReturnFailOnVerifyConnectionError() {
    var errorDetails = AwsErrorDetails.builder().errorMessage("Connection refused").build();
    when(sesClient.listEmailIdentities(any(ListEmailIdentitiesRequest.class)))
        .thenThrow(
            (SesV2Exception)
                SesV2Exception.builder().awsErrorDetails(errorDetails).statusCode(500).build());

    var result = adapter.verify();

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("SES.API_ERROR");
  }

  @Test
  void shouldCreateAdapterWithFactory() {
    var adapterInstance = SesEmailAdapter.create(sesClient);
    assertThat(adapterInstance).isNotNull();
    assertThat(adapterInstance).isInstanceOf(EmailPort.class);
  }
}
