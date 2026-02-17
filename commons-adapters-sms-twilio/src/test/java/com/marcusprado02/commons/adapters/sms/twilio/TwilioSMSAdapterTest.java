package com.marcusprado02.commons.adapters.sms.twilio;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.sms.*;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.rest.api.v2010.account.MessageFetcher;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TwilioSMSAdapterTest {

  private TwilioConfiguration configuration;
  private TwilioSMSAdapter smsAdapter;

  @BeforeEach
  void setUp() {
    configuration = TwilioConfiguration.builder()
        .accountSid("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
        .authToken("test-auth-token")
        .fromPhoneNumber("+1234567890")
        .requestTimeout(Duration.ofSeconds(10))
        .build();

    smsAdapter = new TwilioSMSAdapter(configuration);
  }

  @Test
  @DisplayName("Should send SMS successfully")
  void shouldSendSMS() {
    // Given
    SMS sms = SMS.builder()
        .from("+1234567890")
        .to("+0987654321")
        .message("Test SMS message")
        .build();

    // Mock Twilio Message
    Message mockMessage = mock(Message.class);
    when(mockMessage.getSid()).thenReturn("SMxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    when(mockMessage.getStatus()).thenReturn(Message.Status.SENT);

    // Mock static Message.creator method
    try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
      var mockCreator = mock(MessageCreator.class);
      when(mockCreator.create()).thenReturn(mockMessage);
      mockedMessage.when(() -> Message.creator(any(com.twilio.type.PhoneNumber.class), any(com.twilio.type.PhoneNumber.class), anyString()))
          .thenReturn(mockCreator);

      // When
      Result<SMSPort.SMSReceipt> result = smsAdapter.send(sms);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isNotNull();
      assertThat(result.getOrNull().messageId()).isEqualTo("SMxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
      assertThat(result.getOrNull().status()).isEqualTo("SENT");
      assertThat(result.getOrNull().to().toE164()).isEqualTo("+0987654321");
    }
  }

  @Test
  @DisplayName("Should handle Twilio exception during SMS send")
  void shouldHandleTwilioException() {
    // Given
    SMS sms = SMS.builder()
        .from("+1234567890")
        .to("+invalid")
        .message("Test SMS")
        .build();

    // Mock Twilio exception
    com.twilio.exception.TwilioException twilioException =
        mock(com.twilio.exception.TwilioException.class);
    when(twilioException.getMessage()).thenReturn("Invalid phone number");

    try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
      var mockCreator = mock(MessageCreator.class);
      when(mockCreator.create()).thenThrow(twilioException);
      mockedMessage.when(() -> Message.creator(any(com.twilio.type.PhoneNumber.class), any(com.twilio.type.PhoneNumber.class), anyString()))
          .thenReturn(mockCreator);

      // When
      Result<SMSPort.SMSReceipt> result = smsAdapter.send(sms);

      // Then
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull()).isNotNull();
      assertThat(result.problemOrNull().code().value()).isEqualTo("TWILIO_INVALID_PHONE_NUMBER");
    }
  }

  @Test
  @DisplayName("Should send bulk SMS successfully")
  void shouldSendBulkSMS() {
    // Given
    BulkSMS bulkSMS = BulkSMS.builder()
        .from("+1234567890")
        .to("+0987654321")
        .to("+1122334455")
        .message("Bulk SMS test message")
        .build();

    // Mock successful messages
    Message mockMessage1 = mock(Message.class);
    when(mockMessage1.getSid()).thenReturn("SM1xxxxxxxxxxxxxxxxxxxxxxxxxxx");
    when(mockMessage1.getStatus()).thenReturn(Message.Status.SENT);

    Message mockMessage2 = mock(Message.class);
    when(mockMessage2.getSid()).thenReturn("SM2xxxxxxxxxxxxxxxxxxxxxxxxxxx");
    when(mockMessage2.getStatus()).thenReturn(Message.Status.SENT);

    try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
      var mockCreator1 = mock(MessageCreator.class);
      var mockCreator2 = mock(MessageCreator.class);

      when(mockCreator1.create()).thenReturn(mockMessage1);
      when(mockCreator2.create()).thenReturn(mockMessage2);

      mockedMessage.when(() -> Message.creator(
          eq(new com.twilio.type.PhoneNumber("+0987654321")),
          any(com.twilio.type.PhoneNumber.class),
          anyString()))
          .thenReturn(mockCreator1);

      mockedMessage.when(() -> Message.creator(
          eq(new com.twilio.type.PhoneNumber("+1122334455")),
          any(com.twilio.type.PhoneNumber.class),
          anyString()))
          .thenReturn(mockCreator2);

      // When
      Result<SMSPort.BulkSMSReceipt> result = smsAdapter.sendBulk(bulkSMS);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isNotNull();
      assertThat(result.getOrNull().totalMessages()).isEqualTo(2);
      assertThat(result.getOrNull().successCount()).isEqualTo(2);
      assertThat(result.getOrNull().failureCount()).isEqualTo(0);
    }
  }

  @Test
  @DisplayName("Should get SMS status successfully")
  void shouldGetSMSStatus() {
    // Given
    String messageId = "SMxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    Message mockMessage = mock(Message.class);
    when(mockMessage.getStatus()).thenReturn(Message.Status.DELIVERED);

    var mockFetcher = mock(MessageFetcher.class);
    when(mockFetcher.fetch()).thenReturn(mockMessage);

    try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
      mockedMessage.when(() -> Message.fetcher(messageId)).thenReturn(mockFetcher);

      // When
      Result<SMSPort.SMSStatus> result = smsAdapter.getStatus(messageId);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isEqualTo(SMSPort.SMSStatus.DELIVERED);
    }
  }

  @Test
  @DisplayName("Should verify Twilio connection")
  void shouldVerifyConnection() {
    // Given
    com.twilio.rest.api.v2010.account.ValidationRequest mockValidation =
        mock(com.twilio.rest.api.v2010.account.ValidationRequest.class);

    var mockCreator =
        mock(com.twilio.rest.api.v2010.account.ValidationRequestCreator.class);
    when(mockCreator.create()).thenReturn(mockValidation);

    try (MockedStatic<com.twilio.rest.api.v2010.account.ValidationRequest> mockedValidation =
        mockStatic(com.twilio.rest.api.v2010.account.ValidationRequest.class)) {

      mockedValidation.when(() ->
          com.twilio.rest.api.v2010.account.ValidationRequest.creator(any()))
          .thenReturn(mockCreator);

      // When
      Result<Void> result = smsAdapter.verify();

      // Then
      assertThat(result.isOk()).isTrue();
    }
  }

  @Test
  @DisplayName("Should handle MMS not supported with binary content")
  void shouldHandleMMSNotSupported() {
    // Given
    MMS mms = MMS.builder()
        .from("+1234567890")
        .to("+0987654321")
        .message("MMS with image")
        .addImage("test image content".getBytes(), "jpeg")
        .build();

    // When
    Result<SMSPort.SMSReceipt> result = smsAdapter.sendMMS(mms);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull()).isNotNull();
    assertThat(result.problemOrNull().code().value()).isEqualTo("MMS_MEDIA_NOT_SUPPORTED");
  }
}
