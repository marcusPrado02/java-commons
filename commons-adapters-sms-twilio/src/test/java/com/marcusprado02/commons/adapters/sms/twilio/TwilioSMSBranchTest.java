package com.marcusprado02.commons.adapters.sms.twilio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.ports.sms.SMS;
import com.marcusprado02.commons.ports.sms.SMSPort;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.rest.api.v2010.account.MessageFetcher;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class TwilioSMSBranchTest {

  private static TwilioConfiguration baseConfig() {
    return TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("test-auth-token")
        .fromPhoneNumber("+1234567890")
        .requestTimeout(Duration.ofSeconds(10))
        .build();
  }

  private static TwilioConfiguration configWithWebhook() {
    return TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("test-auth-token")
        .fromPhoneNumber("+1234567890")
        .requestTimeout(Duration.ofSeconds(10))
        .deliveryReceiptsEnabled(true)
        .webhookUrl("https://example.com/webhook")
        .build();
  }

  private static TwilioConfiguration configReceiptsEnabledNullWebhook() {
    return TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("test-auth-token")
        .fromPhoneNumber("+1234567890")
        .requestTimeout(Duration.ofSeconds(10))
        .deliveryReceiptsEnabled(true)
        .build();
  }

  private static SMS testSms() {
    return SMS.builder().from("+1234567890").to("+19876543210").message("hi").build();
  }

  // --- mapTwilioException: Authentication/20003 branch ---

  @Test
  void send_authenticationException_returnsAuthError() {
    var adapter = new TwilioSmsAdapter(baseConfig());
    com.twilio.exception.TwilioException ex = mock(com.twilio.exception.TwilioException.class);
    when(ex.getMessage()).thenReturn("Authentication failed: 20003");

    try (var staticMock = mockStatic(Message.class)) {
      var mockCreator = mock(MessageCreator.class);
      when(mockCreator.create()).thenThrow(ex);
      staticMock
          .when(
              () ->
                  Message.creator(
                      any(com.twilio.type.PhoneNumber.class),
                      any(com.twilio.type.PhoneNumber.class),
                      anyString()))
          .thenReturn(mockCreator);

      var result = adapter.send(testSms());
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("TWILIO_AUTHENTICATION_ERROR");
    }
  }

  // --- mapTwilioException: null message → default TWILIO_API_ERROR ---

  @Test
  void send_nullExceptionMessage_returnsGenericApiError() {
    var adapter = new TwilioSmsAdapter(baseConfig());
    com.twilio.exception.TwilioException ex = mock(com.twilio.exception.TwilioException.class);
    when(ex.getMessage()).thenReturn(null);

    try (var staticMock = mockStatic(Message.class)) {
      var mockCreator = mock(MessageCreator.class);
      when(mockCreator.create()).thenThrow(ex);
      staticMock
          .when(
              () ->
                  Message.creator(
                      any(com.twilio.type.PhoneNumber.class),
                      any(com.twilio.type.PhoneNumber.class),
                      anyString()))
          .thenReturn(mockCreator);

      var result = adapter.send(testSms());
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("TWILIO_API_ERROR");
    }
  }

  // --- send: deliveryReceiptsEnabled=true + webhookUrl != null → body executed ---

  @Test
  void send_deliveryReceiptsWithWebhook_setsStatusCallback() {
    var adapter = new TwilioSmsAdapter(configWithWebhook());

    Message mockMessage = mock(Message.class);
    when(mockMessage.getSid()).thenReturn("SM1");
    when(mockMessage.getStatus()).thenReturn(Message.Status.SENT);

    try (var staticMock = mockStatic(Message.class)) {
      var mockCreator = mock(MessageCreator.class);
      when(mockCreator.setStatusCallback(any())).thenReturn(mockCreator);
      when(mockCreator.create()).thenReturn(mockMessage);
      staticMock
          .when(
              () ->
                  Message.creator(
                      any(com.twilio.type.PhoneNumber.class),
                      any(com.twilio.type.PhoneNumber.class),
                      anyString()))
          .thenReturn(mockCreator);

      var result = adapter.send(testSms());
      assertThat(result.isOk()).isTrue();
    }
  }

  // --- send: deliveryReceiptsEnabled=true + webhookUrl=null → body skipped ---

  @Test
  void send_deliveryReceiptsEnabledNullWebhook_skipsStatusCallback() {
    var adapter = new TwilioSmsAdapter(configReceiptsEnabledNullWebhook());

    Message mockMessage = mock(Message.class);
    when(mockMessage.getSid()).thenReturn("SM2");
    when(mockMessage.getStatus()).thenReturn(Message.Status.SENT);

    try (var staticMock = mockStatic(Message.class)) {
      var mockCreator = mock(MessageCreator.class);
      when(mockCreator.create()).thenReturn(mockMessage);
      staticMock
          .when(
              () ->
                  Message.creator(
                      any(com.twilio.type.PhoneNumber.class),
                      any(com.twilio.type.PhoneNumber.class),
                      anyString()))
          .thenReturn(mockCreator);

      var result = adapter.send(testSms());
      assertThat(result.isOk()).isTrue();
    }
  }

  // --- mapTwilioStatus: null → UNKNOWN ---

  @Test
  void getStatus_nullStatus_returnsUnknown() {
    var adapter = new TwilioSmsAdapter(baseConfig());
    Message mockMessage = mock(Message.class);
    when(mockMessage.getStatus()).thenReturn(null);
    var mockFetcher = mock(MessageFetcher.class);
    when(mockFetcher.fetch()).thenReturn(mockMessage);

    try (var staticMock = mockStatic(Message.class)) {
      staticMock.when(() -> Message.fetcher(anyString())).thenReturn(mockFetcher);
      var result = adapter.getStatus("SM123");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isEqualTo(SMSPort.SMSStatus.UNKNOWN);
    }
  }

  // --- mapTwilioStatus: ACCEPTED → QUEUED ---

  @Test
  void getStatus_accepted_returnsQueued() {
    var adapter = new TwilioSmsAdapter(baseConfig());
    Message mockMessage = mock(Message.class);
    when(mockMessage.getStatus()).thenReturn(Message.Status.ACCEPTED);
    var mockFetcher = mock(MessageFetcher.class);
    when(mockFetcher.fetch()).thenReturn(mockMessage);

    try (var staticMock = mockStatic(Message.class)) {
      staticMock.when(() -> Message.fetcher(anyString())).thenReturn(mockFetcher);
      var result = adapter.getStatus("SM123");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isEqualTo(SMSPort.SMSStatus.QUEUED);
    }
  }

  // --- mapTwilioStatus: QUEUED → QUEUED ---

  @Test
  void getStatus_queued_returnsQueued() {
    var adapter = new TwilioSmsAdapter(baseConfig());
    Message mockMessage = mock(Message.class);
    when(mockMessage.getStatus()).thenReturn(Message.Status.QUEUED);
    var mockFetcher = mock(MessageFetcher.class);
    when(mockFetcher.fetch()).thenReturn(mockMessage);

    try (var staticMock = mockStatic(Message.class)) {
      staticMock.when(() -> Message.fetcher(anyString())).thenReturn(mockFetcher);
      var result = adapter.getStatus("SM123");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isEqualTo(SMSPort.SMSStatus.QUEUED);
    }
  }

  // --- mapTwilioStatus: SENDING → SENDING ---

  @Test
  void getStatus_sending_returnsSending() {
    var adapter = new TwilioSmsAdapter(baseConfig());
    Message mockMessage = mock(Message.class);
    when(mockMessage.getStatus()).thenReturn(Message.Status.SENDING);
    var mockFetcher = mock(MessageFetcher.class);
    when(mockFetcher.fetch()).thenReturn(mockMessage);

    try (var staticMock = mockStatic(Message.class)) {
      staticMock.when(() -> Message.fetcher(anyString())).thenReturn(mockFetcher);
      var result = adapter.getStatus("SM123");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isEqualTo(SMSPort.SMSStatus.SENDING);
    }
  }

  // --- mapTwilioStatus: SENT → SENT ---

  @Test
  void getStatus_sent_returnsSent() {
    var adapter = new TwilioSmsAdapter(baseConfig());
    Message mockMessage = mock(Message.class);
    when(mockMessage.getStatus()).thenReturn(Message.Status.SENT);
    var mockFetcher = mock(MessageFetcher.class);
    when(mockFetcher.fetch()).thenReturn(mockMessage);

    try (var staticMock = mockStatic(Message.class)) {
      staticMock.when(() -> Message.fetcher(anyString())).thenReturn(mockFetcher);
      var result = adapter.getStatus("SM123");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isEqualTo(SMSPort.SMSStatus.SENT);
    }
  }

  // --- mapTwilioStatus: FAILED → FAILED ---

  @Test
  void getStatus_failed_returnsFailed() {
    var adapter = new TwilioSmsAdapter(baseConfig());
    Message mockMessage = mock(Message.class);
    when(mockMessage.getStatus()).thenReturn(Message.Status.FAILED);
    var mockFetcher = mock(MessageFetcher.class);
    when(mockFetcher.fetch()).thenReturn(mockMessage);

    try (var staticMock = mockStatic(Message.class)) {
      staticMock.when(() -> Message.fetcher(anyString())).thenReturn(mockFetcher);
      var result = adapter.getStatus("SM123");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isEqualTo(SMSPort.SMSStatus.FAILED);
    }
  }

  // --- mapTwilioStatus: UNDELIVERED → FAILED ---

  @Test
  void getStatus_undelivered_returnsFailed() {
    var adapter = new TwilioSmsAdapter(baseConfig());
    Message mockMessage = mock(Message.class);
    when(mockMessage.getStatus()).thenReturn(Message.Status.UNDELIVERED);
    var mockFetcher = mock(MessageFetcher.class);
    when(mockFetcher.fetch()).thenReturn(mockMessage);

    try (var staticMock = mockStatic(Message.class)) {
      staticMock.when(() -> Message.fetcher(anyString())).thenReturn(mockFetcher);
      var result = adapter.getStatus("SM123");
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isEqualTo(SMSPort.SMSStatus.FAILED);
    }
  }
}
