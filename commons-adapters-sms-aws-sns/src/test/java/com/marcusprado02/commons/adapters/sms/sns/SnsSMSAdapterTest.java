package com.marcusprado02.commons.adapters.sms.sns;

import com.marcusprado02.commons.ports.sms.*;
import com.marcusprado02.commons.kernel.result.Result;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;
import software.amazon.awssdk.services.sns.model.*;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnsSMSAdapterTest {

  @Mock
  private SnsClient snsClient;

  @Mock
  private SnsClientBuilder snsClientBuilder;

  private SnsConfiguration configuration;
  private SnsSMSAdapter adapter;

  @BeforeEach
  void setUp() {
    configuration = SnsConfiguration.builder()
        .region(Region.US_EAST_1)
        .accessKeyId("AKIAIOSFODNN7EXAMPLE")
        .secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
        .requestTimeout(Duration.ofSeconds(15))
        .maxPriceUSD(1.0)
        .build();
  }

  @Test
  void shouldSendSMSSuccessfully() {
    // Given
    try (MockedStatic<SnsClient> snsClientMock = mockStatic(SnsClient.class)) {
      snsClientMock.when(SnsClient::builder).thenReturn(snsClientBuilder);
      when(snsClientBuilder.region(any(Region.class))).thenReturn(snsClientBuilder);
      when(snsClientBuilder.credentialsProvider(any())).thenReturn(snsClientBuilder);
      when(snsClientBuilder.overrideConfiguration(any())).thenReturn(snsClientBuilder);
      when(snsClientBuilder.build()).thenReturn(snsClient);

      PublishResponse response = PublishResponse.builder()
          .messageId("12345-67890")
          .build();
      when(snsClient.publish(any(PublishRequest.class))).thenReturn(response);

      adapter = new SnsSMSAdapter(configuration);

      PhoneNumber phone = PhoneNumber.of("+1234567890");
      SMS sms = SMS.of(phone, "Test message");

      // When
      Result<SMSReceipt> result = adapter.send(sms);

      // Then
      assertTrue(result.isOk());
      SMSReceipt receipt = result.getOrNull();
      assertEquals("12345-67890", receipt.messageId());
      assertEquals(phone, receipt.to());
      assertEquals(SMSStatus.SENT, receipt.status());
      assertNotNull(receipt.sentAt());

      verify(snsClient).publish(any(PublishRequest.class));
    }
  }

  @Test
  void shouldSendSMSWithOptions() {
    // Given
    try (MockedStatic<SnsClient> snsClientMock = mockStatic(SnsClient.class)) {
      setupMockedClient(snsClientMock);

      PublishResponse response = PublishResponse.builder()
          .messageId("msg-123")
          .build();
      when(snsClient.publish(any(PublishRequest.class))).thenReturn(response);

      adapter = new SnsSMSAdapter(configuration);

      PhoneNumber phone = PhoneNumber.of("+1234567890");
      SMS sms = SMS.of(phone, "Test with options");
      SMSOptions options = SMSOptions.builder()
          .deliveryReceipt(true)
          .priority(SMSOptions.SMSPriority.HIGH)
          .build();

      // When
      Result<SMSReceipt> result = adapter.send(sms, options);

      // Then
      assertTrue(result.isOk());
      assertEquals("msg-123", result.getOrNull().messageId());
    }
  }

  @Test
  void shouldHandleInvalidParameterException() {
    // Given
    try (MockedStatic<SnsClient> snsClientMock = mockStatic(SnsClient.class)) {
      setupMockedClient(snsClientMock);

      InvalidParameterException exception = InvalidParameterException.builder()
          .message("Invalid phone number")
          .build();
      when(snsClient.publish(any(PublishRequest.class))).thenThrow(exception);

      adapter = new SnsSMSAdapter(configuration);

      SMS sms = SMS.of(PhoneNumber.of("+1234567890"), "Test");

      // When
      Result<SMSReceipt> result = adapter.send(sms);

      // Then
      assertTrue(result.isFail());
      assertEquals("INVALID_PARAMETER", result.problemOrNull().code().value());
      assertTrue(result.problemOrNull().message().contains("Invalid phone number"));
    }
  }

  @Test
  void shouldHandleAuthorizationError() {
    // Given
    try (MockedStatic<SnsClient> snsClientMock = mockStatic(SnsClient.class)) {
      setupMockedClient(snsClientMock);

      AuthorizationErrorException exception = AuthorizationErrorException.builder()
          .message("Access denied")
          .build();
      when(snsClient.publish(any(PublishRequest.class))).thenThrow(exception);

      adapter = new SnsSMSAdapter(configuration);

      SMS sms = SMS.of(PhoneNumber.of("+1234567890"), "Test");

      // When
      Result<SMSReceipt> result = adapter.send(sms);

      // Then
      assertTrue(result.isFail());
      assertEquals("AUTHORIZATION_ERROR", result.problemOrNull().code().value());
    }
  }

  @Test
  void shouldHandleOptedOutException() {
    // Given
    try (MockedStatic<SnsClient> snsClientMock = mockStatic(SnsClient.class)) {
      setupMockedClient(snsClientMock);

      OptedOutException exception = OptedOutException.builder()
          .message("Phone number opted out")
          .build();
      when(snsClient.publish(any(PublishRequest.class))).thenThrow(exception);

      adapter = new SnsSMSAdapter(configuration);

      SMS sms = SMS.of(PhoneNumber.of("+1234567890"), "Test");

      // When
      Result<SMSReceipt> result = adapter.send(sms);

      // Then
      assertTrue(result.isFail());
      assertEquals("PHONE_OPTED_OUT", result.problemOrNull().code().value());
    }
  }

  @Test
  void shouldSendBulkSMSSuccessfully() {
    // Given
    try (MockedStatic<SnsClient> snsClientMock = mockStatic(SnsClient.class)) {
      setupMockedClient(snsClientMock);

      PublishResponse response1 = PublishResponse.builder().messageId("msg-1").build();
      PublishResponse response2 = PublishResponse.builder().messageId("msg-2").build();
      when(snsClient.publish(any(PublishRequest.class)))
          .thenReturn(response1)
          .thenReturn(response2);

      adapter = new SnsSMSAdapter(configuration);

      List<PhoneNumber> recipients = List.of(
          PhoneNumber.of("+1234567890"),
          PhoneNumber.of("+1987654321")
      );
      BulkSMS bulkSMS = BulkSMS.of(recipients, "Bulk message");

      // When
      Result<BulkSMSReceipt> result = adapter.sendBulk(bulkSMS);

      // Then
      assertTrue(result.isOk());
      BulkSMSReceipt receipt = result.getOrNull();
      assertEquals(2, receipt.successfulReceipts().size());
      assertEquals(0, receipt.failedReceipts().size());

      verify(snsClient, times(2)).publish(any(PublishRequest.class));
    }
  }

  @Test
  void shouldHandleMixedBulkResults() {
    // Given
    try (MockedStatic<SnsClient> snsClientMock = mockStatic(SnsClient.class)) {
      setupMockedClient(snsClientMock);

      PublishResponse successResponse = PublishResponse.builder().messageId("msg-1").build();
      InvalidParameterException exception = InvalidParameterException.builder()
          .message("Invalid phone")
          .build();

      when(snsClient.publish(any(PublishRequest.class)))
          .thenReturn(successResponse)
          .thenThrow(exception);

      adapter = new SnsSMSAdapter(configuration);

      List<PhoneNumber> recipients = List.of(
          PhoneNumber.of("+1234567890"),
          PhoneNumber.of("invalid")
      );
      BulkSMS bulkSMS = BulkSMS.of(recipients, "Bulk message");

      // When
      Result<BulkSMSReceipt> result = adapter.sendBulk(bulkSMS);

      // Then
      assertTrue(result.isOk());
      BulkSMSReceipt receipt = result.getOrNull();
      assertEquals(1, receipt.successfulReceipts().size());
      assertEquals(1, receipt.failedReceipts().size());
    }
  }

  @Test
  void shouldRejectMMSWithNotSupportedError() {
    // Given
    adapter = new SnsSMSAdapter(configuration);

    MMS mms = MMS.builder()
        .to(PhoneNumber.of("+1234567890"))
        .message("Test MMS")
        .build();

    // When
    Result<SMSReceipt> result = adapter.sendMMS(mms);

    // Then
    assertTrue(result.isFail());
    assertEquals("MMS_NOT_SUPPORTED", result.problemOrNull().code().value());
    assertTrue(result.problemOrNull().message().contains("AWS SNS does not support MMS"));
  }

  @Test
  void shouldVerifyConnectionSuccessfully() {
    // Given
    try (MockedStatic<SnsClient> snsClientMock = mockStatic(SnsClient.class)) {
      setupMockedClient(snsClientMock);

      GetSMSAttributesResponse response = GetSMSAttributesResponse.builder().build();
      when(snsClient.getSMSAttributes(any(GetSMSAttributesRequest.class))).thenReturn(response);

      adapter = new SnsSMSAdapter(configuration);

      // When
      Result<Boolean> result = adapter.verify();

      // Then
      assertTrue(result.isOk());
      assertTrue(result.getOrNull());
      verify(snsClient).getSMSAttributes(any(GetSMSAttributesRequest.class));
    }
  }

  @Test
  void shouldHandleVerificationFailure() {
    // Given
    try (MockedStatic<SnsClient> snsClientMock = mockStatic(SnsClient.class)) {
      setupMockedClient(snsClientMock);

      AuthorizationErrorException exception = AuthorizationErrorException.builder()
          .message("Access denied")
          .build();
      when(snsClient.getSMSAttributes(any(GetSMSAttributesRequest.class))).thenThrow(exception);

      adapter = new SnsSMSAdapter(configuration);

      // When
      Result<Boolean> result = adapter.verify();

      // Then
      assertTrue(result.isFail());
      assertEquals("AUTHORIZATION_ERROR", result.problemOrNull().code().value());
    }
  }

  @Test
  void shouldReturnUnknownStatusForMessageTracking() {
    // Given
    adapter = new SnsSMSAdapter(configuration);

    // When
    Result<SMSStatus> result = adapter.getStatus("msg-123");

    // Then
    assertTrue(result.isOk());
    assertEquals(SMSStatus.UNKNOWN, result.getOrNull());
  }

  @Test
  void shouldCloseClientProperly() {
    // Given
    try (MockedStatic<SnsClient> snsClientMock = mockStatic(SnsClient.class)) {
      setupMockedClient(snsClientMock);
      adapter = new SnsSMSAdapter(configuration);

      // When
      adapter.close();

      // Then
      verify(snsClient).close();
    }
  }

  private void setupMockedClient(MockedStatic<SnsClient> snsClientMock) {
    snsClientMock.when(SnsClient::builder).thenReturn(snsClientBuilder);
    when(snsClientBuilder.region(any(Region.class))).thenReturn(snsClientBuilder);
    when(snsClientBuilder.credentialsProvider(any())).thenReturn(snsClientBuilder);
    when(snsClientBuilder.overrideConfiguration(any())).thenReturn(snsClientBuilder);
    when(snsClientBuilder.build()).thenReturn(snsClient);
  }
}
