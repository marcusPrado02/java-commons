package com.marcusprado02.commons.adapters.email.ses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.ports.email.Email;
import com.marcusprado02.commons.ports.email.TemplateEmailRequest;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.ListEmailIdentitiesRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

class SesBranchTest {

  // --- close() branch ---

  @Test
  void close_callsClientClose() {
    SesV2Client client = mock(SesV2Client.class);
    SesEmailAdapter adapter = SesEmailAdapter.create(client);
    adapter.close();
    verify(client).close();
  }

  // --- send() generic Exception catch ---

  @Test
  void send_genericException_returnsFailure() {
    SesV2Client client = mock(SesV2Client.class);
    when(client.sendEmail(any(SendEmailRequest.class))).thenThrow(new RuntimeException("boom"));

    SesEmailAdapter adapter = SesEmailAdapter.create(client);
    var email =
        Email.builder().from("a@b.com").to("c@d.com").subject("test").textContent("body").build();

    var result = adapter.send(email);
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("SES.SEND_ERROR");
  }

  // --- send() SesV2Exception 400 (BAD_REQUEST) and 404 (NOT_FOUND) branches ---

  @Test
  void send_ses400_returnsBadRequest() {
    SesV2Client client = mock(SesV2Client.class);
    var errorDetails = AwsErrorDetails.builder().errorMessage("Bad request").build();
    when(client.sendEmail(any(SendEmailRequest.class)))
        .thenThrow(
            (SesV2Exception)
                SesV2Exception.builder().awsErrorDetails(errorDetails).statusCode(400).build());

    SesEmailAdapter adapter = SesEmailAdapter.create(client);
    var email = Email.builder().from("a@b.com").to("b@c.com").subject("s").textContent("t").build();

    var result = adapter.send(email);
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("SES.BAD_REQUEST");
  }

  @Test
  void send_ses404_returnsNotFound() {
    SesV2Client client = mock(SesV2Client.class);
    var errorDetails = AwsErrorDetails.builder().errorMessage("Not found").build();
    when(client.sendEmail(any(SendEmailRequest.class)))
        .thenThrow(
            (SesV2Exception)
                SesV2Exception.builder().awsErrorDetails(errorDetails).statusCode(404).build());

    SesEmailAdapter adapter = SesEmailAdapter.create(client);
    var email = Email.builder().from("a@b.com").to("b@c.com").subject("s").textContent("t").build();

    var result = adapter.send(email);
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("SES.NOT_FOUND");
  }

  // --- verify() generic Exception catch ---

  @Test
  void verify_genericException_returnsFailure() {
    SesV2Client client = mock(SesV2Client.class);
    when(client.listEmailIdentities(any(ListEmailIdentitiesRequest.class)))
        .thenThrow(new RuntimeException("connection refused"));

    SesEmailAdapter adapter = SesEmailAdapter.create(client);
    var result = adapter.verify();
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("SES.CONNECTION_FAILED");
  }

  // --- sendWithTemplate() exception branches ---

  @Test
  void sendWithTemplate_sesException_returnsFailure() {
    SesV2Client client = mock(SesV2Client.class);
    var errorDetails = AwsErrorDetails.builder().errorMessage("Template error").build();
    when(client.sendEmail(any(SendEmailRequest.class)))
        .thenThrow(
            (SesV2Exception)
                SesV2Exception.builder().awsErrorDetails(errorDetails).statusCode(400).build());

    SesEmailAdapter adapter = SesEmailAdapter.create(client);
    var request =
        TemplateEmailRequest.builder()
            .from("a@b.com")
            .to("b@c.com")
            .subject("s")
            .templateName("MyTemplate")
            .build();

    var result = adapter.sendWithTemplate(request);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void sendWithTemplate_genericException_returnsFailure() {
    SesV2Client client = mock(SesV2Client.class);
    when(client.sendEmail(any(SendEmailRequest.class))).thenThrow(new RuntimeException("boom"));

    SesEmailAdapter adapter = SesEmailAdapter.create(client);
    var request =
        TemplateEmailRequest.builder()
            .from("a@b.com")
            .to("b@c.com")
            .subject("s")
            .templateName("MyTemplate")
            .build();

    var result = adapter.sendWithTemplate(request);
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("SES.TEMPLATE_SEND_ERROR");
  }
}
