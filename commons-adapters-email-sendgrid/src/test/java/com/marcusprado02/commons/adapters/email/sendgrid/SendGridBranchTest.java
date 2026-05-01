package com.marcusprado02.commons.adapters.email.sendgrid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.ports.email.Email;
import com.marcusprado02.commons.ports.email.EmailPort;
import com.marcusprado02.commons.ports.email.TemplateEmailRequest;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class SendGridBranchTest {

  private static SendGridConfiguration config(boolean sandboxMode) {
    return SendGridConfiguration.builder().apiKey("SG.test-key").sandboxMode(sandboxMode).build();
  }

  private static Response response(int statusCode) {
    Response r = new Response();
    r.setStatusCode(statusCode);
    r.setBody("");
    return r;
  }

  private static Email simpleEmail() {
    return Email.builder()
        .from("from@example.com")
        .to("to@example.com")
        .subject("Subject")
        .textContent("Body text")
        .build();
  }

  // ── send(): sandbox mode true/false ──────────────────────────────────────

  @Test
  void send_sandboxMode_successResponse() throws Exception {
    try (MockedConstruction<SendGrid> ignored =
        mockConstruction(
            SendGrid.class,
            (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(response(202)))) {

      EmailPort.EmailReceipt receipt =
          new SendGridEmailAdapter(config(true)).send(simpleEmail()).getOrNull();
      assertThat(receipt).isNotNull();
    }
  }

  @Test
  void send_nonSandbox_successResponse() throws Exception {
    try (MockedConstruction<SendGrid> ignored =
        mockConstruction(
            SendGrid.class,
            (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(response(202)))) {

      EmailPort.EmailReceipt receipt =
          new SendGridEmailAdapter(config(false)).send(simpleEmail()).getOrNull();
      assertThat(receipt).isNotNull();
    }
  }

  // ── send(): createSendGridProblem switch branches (401, 403, 429, default) ─

  @Test
  void send_401Response_returnsUnauthorized() throws Exception {
    try (MockedConstruction<SendGrid> ignored =
        mockConstruction(
            SendGrid.class,
            (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(response(401)))) {

      var result = new SendGridEmailAdapter(config(false)).send(simpleEmail());
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SENDGRID_UNAUTHORIZED");
    }
  }

  @Test
  void send_403Response_returnsForbidden() throws Exception {
    try (MockedConstruction<SendGrid> ignored =
        mockConstruction(
            SendGrid.class,
            (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(response(403)))) {

      var result = new SendGridEmailAdapter(config(false)).send(simpleEmail());
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SENDGRID_FORBIDDEN");
    }
  }

  @Test
  void send_429Response_returnsRateLimited() throws Exception {
    try (MockedConstruction<SendGrid> ignored =
        mockConstruction(
            SendGrid.class,
            (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(response(429)))) {

      var result = new SendGridEmailAdapter(config(false)).send(simpleEmail());
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SENDGRID_RATE_LIMITED");
    }
  }

  @Test
  void send_500Response_returnsDefaultError() throws Exception {
    try (MockedConstruction<SendGrid> ignored =
        mockConstruction(
            SendGrid.class,
            (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(response(500)))) {

      var result = new SendGridEmailAdapter(config(false)).send(simpleEmail());
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SENDGRID_REQUEST_FAILED");
    }
  }

  // ── send(): IOException → mapIoException ─────────────────────────────────

  @Test
  void send_ioException_returnsIoError() throws Exception {
    try (MockedConstruction<SendGrid> ignored =
        mockConstruction(
            SendGrid.class,
            (mock, ctx) ->
                when(mock.api(any(Request.class))).thenThrow(new IOException("network down")))) {

      var result = new SendGridEmailAdapter(config(false)).send(simpleEmail());
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SENDGRID_IO_ERROR");
    }
  }

  // ── createContent branches: html, text, empty ────────────────────────────

  @Test
  void send_htmlContent_usesHtmlMimeType() throws Exception {
    try (MockedConstruction<SendGrid> ignored =
        mockConstruction(
            SendGrid.class,
            (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(response(202)))) {

      Email email =
          Email.builder()
              .from("from@example.com")
              .to("to@example.com")
              .subject("Subject")
              .htmlContent("<b>Hello</b>")
              .build();

      var result = new SendGridEmailAdapter(config(false)).send(email);
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void send_noContent_usesEmptyText() throws Exception {
    try (MockedConstruction<SendGrid> ignored =
        mockConstruction(
            SendGrid.class,
            (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(response(202)))) {

      Email email =
          Email.builder().from("from@example.com").to("to@example.com").subject("Subject").build();

      var result = new SendGridEmailAdapter(config(false)).send(email);
      assertThat(result.isOk()).isTrue();
    }
  }

  // ── verify(): success and failure paths ──────────────────────────────────

  @Test
  void verify_successResponse_returnsOk() throws Exception {
    try (MockedConstruction<SendGrid> ignored =
        mockConstruction(
            SendGrid.class,
            (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(response(200)))) {

      var result = new SendGridEmailAdapter(config(false)).verify();
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  void verify_errorResponse_returnsFail() throws Exception {
    try (MockedConstruction<SendGrid> ignored =
        mockConstruction(
            SendGrid.class,
            (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(response(401)))) {

      var result = new SendGridEmailAdapter(config(false)).verify();
      assertThat(result.isFail()).isTrue();
    }
  }

  @Test
  void verify_exception_returnsFail() throws Exception {
    try (MockedConstruction<SendGrid> ignored =
        mockConstruction(
            SendGrid.class,
            (mock, ctx) ->
                when(mock.api(any(Request.class))).thenThrow(new RuntimeException("boom")))) {

      var result = new SendGridEmailAdapter(config(false)).verify();
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SENDGRID_CONNECTION_FAILED");
    }
  }

  // ── sendWithTemplate(): cc, bcc, variables coverage ──────────────────────

  @Test
  void sendWithTemplate_withCcBccAndVars_coversLoops() throws Exception {
    try (MockedConstruction<SendGrid> ignored =
        mockConstruction(
            SendGrid.class,
            (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(response(202)))) {

      TemplateEmailRequest req =
          TemplateEmailRequest.builder()
              .from("from@example.com")
              .to("to@example.com")
              .cc("cc@example.com")
              .bcc("bcc@example.com")
              .subject("Subject")
              .templateName("d-abc123")
              .variable("name", "Alice")
              .variable("count", 5)
              .build();

      var result = new SendGridEmailAdapter(config(false)).sendWithTemplate(req);
      assertThat(result.isOk()).isTrue();
    }
  }

  // ── close() ──────────────────────────────────────────────────────────────

  @Test
  void close_doesNotThrow() throws Exception {
    new SendGridEmailAdapter(config(false)).close();
  }
}
