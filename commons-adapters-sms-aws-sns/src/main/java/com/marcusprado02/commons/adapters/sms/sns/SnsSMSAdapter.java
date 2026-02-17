package com.marcusprado02.commons.adapters.sms.sns;

import com.marcusprado02.commons.ports.sms.SMSPort;
import com.marcusprado02.commons.ports.sms.*;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.Severity;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.*;

/**
 * AWS SNS implementation of SMSPort.
 *
 * <p>This adapter uses Amazon Simple Notification Service (SNS) to send SMS messages.
 * It supports both individual SMS and bulk operations, with configurable pricing limits
 * and delivery status tracking.
 */
public class SnsSMSAdapter implements SMSPort, AutoCloseable {

  private final SnsClient snsClient;
  private final SnsConfiguration configuration;

  /**
   * Creates a new SnsSMSAdapter with the given configuration.
   *
   * @param configuration SNS configuration
   */
  public SnsSMSAdapter(SnsConfiguration configuration) {
    Objects.requireNonNull(configuration, "Configuration cannot be null");
    this.configuration = configuration;
    this.snsClient = createSnsClient(configuration);
  }

  @Override
  public Result<SMSReceipt> send(SMS sms) {
    return send(sms, SMSOptions.defaults());
  }

  public Result<SMSReceipt> send(SMS sms, SMSOptions options) {
    Objects.requireNonNull(sms, "SMS cannot be null");
    Objects.requireNonNull(options, "SMS options cannot be null");

    try {
      PublishRequest request = buildPublishRequest(sms, options);
      PublishResponse response = snsClient.publish(request);

      SMSReceipt receipt = createReceipt(response, sms.to());
      return Result.ok(receipt);

    } catch (Exception e) {
      Problem problem = mapSnsException(e);
      return Result.fail(problem);
    }
  }

  @Override
  public Result<BulkSMSReceipt> sendBulk(BulkSMS bulkSMS) {
    return sendBulk(bulkSMS, SMSOptions.defaults());
  }

  public Result<BulkSMSReceipt> sendBulk(BulkSMS bulkSMS, SMSOptions options) {
    Objects.requireNonNull(bulkSMS, "Bulk SMS cannot be null");
    Objects.requireNonNull(options, "SMS options cannot be null");

    int successCount = 0;
    int failureCount = 0;

    // Send each SMS individually (SNS doesn't have native bulk operations)
    for (PhoneNumber phone : bulkSMS.to()) {
      SMS sms = SMS.builder()
          .from(bulkSMS.from())
          .to(phone)
          .message(bulkSMS.message())
          .options(options)
          .build();
      Result<SMSReceipt> result = send(sms, options);

      if (result.isOk()) {
        successCount++;
      } else {
        failureCount++;
      }
    }

    BulkSMSReceipt bulkReceipt = SMSPort.BulkSMSReceipt.of(
        bulkSMS.to().size(),
        successCount,
        failureCount
    );

    return Result.ok(bulkReceipt);
  }

  @Override
  public Result<SMSReceipt> sendMMS(MMS mms) {
    return sendMMS(mms, SMSOptions.defaults());
  }

  public Result<SMSReceipt> sendMMS(MMS mms, SMSOptions options) {
    // AWS SNS doesn't natively support MMS, return appropriate error
    Problem problem = Problem.of(
        ErrorCode.of("MMS_NOT_SUPPORTED"),
        ErrorCategory.BUSINESS,
        Severity.WARNING,
        "AWS SNS does not support MMS messages. Use SMS instead or consider using Amazon Pinpoint for MMS support."
    );

    return Result.fail(problem);
  }

  @Override
  public Result<Void> verify() {
    try {
      // Test connection by checking if client is configured (SNS doesn't have a lightweight test operation)
      // We could try to publish to a test number, but that would cost money
      // So we just verify the client is initialized
      if (snsClient == null) {
        throw new IllegalStateException("SNS client not initialized");
      }
      return Result.ok(null);

    } catch (Exception e) {
      Problem problem = mapSnsException(e);
      return Result.fail(problem);
    }
  }

  @Override
  public Result<SMSPort.SMSStatus> getStatus(String messageId) {
    Objects.requireNonNull(messageId, "Message ID cannot be null");

    // AWS SNS doesn't provide direct status checking for SMS
    // Return UNKNOWN status with explanation
    return Result.ok(SMSPort.SMSStatus.UNKNOWN);
  }

  @Override
  public void close() {
    if (snsClient != null) {
      snsClient.close();
    }
  }

  // Private helper methods

  private SnsClient createSnsClient(SnsConfiguration config) {
    ClientOverrideConfiguration clientConfig = ClientOverrideConfiguration.builder()
        .apiCallTimeout(config.requestTimeout())
        .apiCallAttemptTimeout(config.requestTimeout())
        .build();

    var builder = SnsClient.builder()
        .region(config.region())
        .overrideConfiguration(clientConfig);

    // Configure credentials
    if (config.sessionToken() != null && !config.sessionToken().isBlank()) {
      // Use session credentials (temporary)
      AwsSessionCredentials credentials = AwsSessionCredentials.create(
          config.accessKeyId(),
          config.secretAccessKey(),
          config.sessionToken()
      );
      builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
    } else if (config.accessKeyId() != null && !config.accessKeyId().isBlank()) {
      // Use basic credentials
      AwsBasicCredentials credentials = AwsBasicCredentials.create(
          config.accessKeyId(),
          config.secretAccessKey()
      );
      builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
    } else {
      // Use default credential chain (IAM roles, environment variables, etc.)
      builder.credentialsProvider(DefaultCredentialsProvider.create());
    }

    return builder.build();
  }

  private PublishRequest buildPublishRequest(SMS sms, SMSOptions options) {
    Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();

    // Set SMS type
    smsAttributes.put("AWS.SNS.SMS.SMSType", MessageAttributeValue.builder()
        .stringValue(configuration.smsType().name())
        .dataType("String")
        .build());

    // Set max price
    smsAttributes.put("AWS.SNS.SMS.MaxPrice", MessageAttributeValue.builder()
        .stringValue(String.format("%.2f", configuration.maxPriceUSD()))
        .dataType("Number")
        .build());

    // Set sender ID if configured
    if (configuration.defaultSenderId() != null && !configuration.defaultSenderId().isBlank()) {
      smsAttributes.put("AWS.SNS.SMS.SenderID", MessageAttributeValue.builder()
          .stringValue(configuration.defaultSenderId())
          .dataType("String")
          .build());
    }

    // Enable delivery status logging if configured
    if (configuration.deliveryStatusLogging()) {
      smsAttributes.put("AWS.SNS.SMS.DeliveryStatusLogging", MessageAttributeValue.builder()
          .stringValue("true")
          .dataType("String")
          .build());
    }

    return PublishRequest.builder()
        .phoneNumber(sms.to().toE164())
        .message(sms.message())
        .messageAttributes(smsAttributes)
        .build();
  }

  private SMSPort.SMSReceipt createReceipt(PublishResponse response, PhoneNumber to) {
    return SMSPort.SMSReceipt.of(
        response.messageId(),
        "SENT", // SNS returns success on acceptance, not delivery
        to
    );
  }

  private Problem mapSnsException(Exception e) {
    return switch (e) {
      case InvalidParameterException ex -> Problem.of(
          ErrorCode.of("INVALID_PARAMETER"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Invalid parameter provided to SNS: " + ex.getMessage());

      case AuthorizationErrorException ex -> Problem.of(
          ErrorCode.of("AUTHORIZATION_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "AWS credentials or permissions invalid: " + ex.getMessage());

      case ThrottledException ex -> Problem.of(
          ErrorCode.of("RATE_LIMIT_EXCEEDED"),
          ErrorCategory.TECHNICAL,
          Severity.WARNING,
          "SNS rate limit exceeded: " + ex.getMessage());

      case InternalErrorException ex -> Problem.of(
          ErrorCode.of("INTERNAL_SERVER_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "AWS SNS internal error: " + ex.getMessage());

      case OptedOutException ex -> Problem.of(
          ErrorCode.of("PHONE_OPTED_OUT"),
          ErrorCategory.BUSINESS,
          Severity.WARNING,
          "The phone number has opted out of receiving SMS: " + ex.getMessage());

      case InvalidParameterValueException ex -> Problem.of(
          ErrorCode.of("INVALID_PARAMETER_VALUE"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Invalid parameter value: " + ex.getMessage());

      default -> Problem.of(
          ErrorCode.of("SMS_SEND_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to send SMS via SNS: " + e.getMessage());
    };
  }
}
