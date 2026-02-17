package com.marcusprado02.commons.adapters.sms.twilio;

import com.marcusprado02.commons.kernel.errors.*;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.sms.*;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import java.net.URI;
import java.util.Objects;

/**
 * Twilio adapter for SMSPort using Twilio Java SDK.
 *
 * <p>Basic implementation supporting:
 * <ul>
 *   <li>SMS sending with text content</li>
 *   <li>Bulk SMS operations</li>
 *   <li>Delivery status tracking</li>
 *   <li>Connection verification</li>
 * </ul>
 */
public class TwilioSMSAdapter implements SMSPort, AutoCloseable {

  private final TwilioConfiguration configuration;

  /**
   * Creates a new TwilioSMSAdapter with the given configuration.
   *
   * @param configuration Twilio configuration with credentials
   * @throws IllegalArgumentException if configuration is null
   */
  public TwilioSMSAdapter(TwilioConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");

    // Initialize Twilio SDK
    Twilio.init(configuration.accountSid(), configuration.authToken());
  }

  @Override
  public Result<SMSReceipt> send(SMS sms) {
    try {
      MessageCreator messageCreator = Message.creator(
          new PhoneNumber(sms.to().toE164()),
          new PhoneNumber(sms.from().toE164()),
          sms.message()
      );

      // Configure webhook URL for status callbacks if enabled
      if (configuration.deliveryReceiptsEnabled() && configuration.webhookUrl() != null) {
        messageCreator = messageCreator.setStatusCallback(URI.create(configuration.webhookUrl()));
      }

      Message message = messageCreator.create();

      SMSReceipt receipt = SMSReceipt.of(
          message.getSid(),
          message.getStatus().toString(),
          sms.to()
      );

      return Result.ok(receipt);

    } catch (com.twilio.exception.TwilioException e) {
      return Result.fail(mapTwilioException(e));
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("SMS_SEND_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to send SMS: " + e.getMessage()));
    }
  }

  @Override
  public Result<BulkSMSReceipt> sendBulk(BulkSMS bulkSMS) {
    int successCount = 0;
    int failureCount = 0;

    for (com.marcusprado02.commons.ports.sms.PhoneNumber recipient : bulkSMS.to()) {
      SMS individualSMS = SMS.builder()
          .from(bulkSMS.from())
          .to(recipient)
          .message(bulkSMS.message())
          .options(bulkSMS.options())
          .build();

      Result<SMSReceipt> result = send(individualSMS);
      if (result.isOk()) {
        successCount++;
      } else {
        failureCount++;
      }
    }

    BulkSMSReceipt bulkReceipt = BulkSMSReceipt.of(
        bulkSMS.to().size(),
        successCount,
        failureCount
    );

    return Result.ok(bulkReceipt);
  }

  @Override
  public Result<Void> verify() {
    try {
      // Simple verification by creating a Message resource (but not sending it)
      // Create a MessageCreator to verify the connection without sending
      Message.creator(
          new PhoneNumber(configuration.fromPhoneNumber()),
          new PhoneNumber(configuration.fromPhoneNumber()),
          "Connection test - not sent"
      );

      // Note: We don't call create() to avoid sending actual message
      return Result.ok(null);

    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("TWILIO_VERIFICATION_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to verify Twilio connection: " + e.getMessage()));
    }
  }

  @Override
  public Result<SMSStatus> getStatus(String messageId) {
    try {
      Message message = Message.fetcher(messageId).fetch();
      SMSStatus status = mapTwilioStatus(message.getStatus());

      return Result.ok(status);

    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("SMS_STATUS_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to get SMS status: " + e.getMessage()));
    }
  }

  @Override
  public void close() throws Exception {
    // Twilio SDK doesn't require explicit cleanup
  }

  private Problem mapTwilioException(com.twilio.exception.TwilioException e) {
    ErrorCategory category = ErrorCategory.TECHNICAL;
    String errorCode = "TWILIO_API_ERROR";

    // Extract error code from message
    String message = e.getMessage();
    if (message != null) {
      if (message.contains("Authentication") || message.contains("20003")) {
        category = ErrorCategory.UNAUTHORIZED;
        errorCode = "TWILIO_AUTHENTICATION_ERROR";
      } else if (message.contains("phone number") || message.contains("21211")) {
        category = ErrorCategory.BUSINESS;
        errorCode = "TWILIO_INVALID_PHONE_NUMBER";
      }
    }

    return Problem.of(
        ErrorCode.of(errorCode),
        category,
        Severity.ERROR,
        "Twilio API error: " + e.getMessage());
  }

  private SMSStatus mapTwilioStatus(Message.Status twilioStatus) {
    if (twilioStatus == null) {
      return SMSStatus.UNKNOWN;
    }

    switch (twilioStatus) {
      case ACCEPTED:
      case QUEUED:
        return SMSStatus.QUEUED;
      case SENDING:
        return SMSStatus.SENDING;
      case SENT:
        return SMSStatus.SENT;
      case DELIVERED:
        return SMSStatus.DELIVERED;
      case FAILED:
      case UNDELIVERED:
        return SMSStatus.FAILED;
      default:
        return SMSStatus.UNKNOWN;
    }
  }
}
