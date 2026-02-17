package com.marcusprado02.commons.ports.sms;

import com.marcusprado02.commons.kernel.result.Result;

/**
 * Port for SMS operations.
 *
 * <p>Abstraction for sending SMS messages through various providers (Twilio, AWS SNS, Azure
 * Communication Services, etc.).
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Send SMS messages with text content
 *   <li>Support MMS with media content
 *   <li>Support bulk messaging
 *   <li>Handle international phone numbers
 *   <li>Status tracking and delivery receipts
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SMS sms = SMS.builder()
 *     .from("+1234567890")
 *     .to("+0987654321")
 *     .message("Welcome! Your account has been created successfully.")
 *     .build();
 *
 * Result<SMSReceipt> result = smsPort.send(sms);
 * }</pre>
 */
public interface SMSPort {

  /**
   * Send an SMS message.
   *
   * @param sms the SMS to send
   * @return Result with receipt or problem
   */
  Result<SMSReceipt> send(SMS sms);

  /**
   * Send an MMS message with media content.
   *
   * <p>MMS support is adapter-specific. Consult your adapter's documentation for supported media
   * types and size limits.
   *
   * @param mms the MMS to send
   * @return Result with receipt or problem
   */
  default Result<SMSReceipt> sendMMS(MMS mms) {
    throw new UnsupportedOperationException("MMS support not implemented by this adapter");
  }

  /**
   * Send a bulk SMS to multiple recipients.
   *
   * <p>Implementation may send messages individually or use bulk API if available.
   *
   * @param bulkSMS the bulk SMS request
   * @return Result with bulk receipt or problem
   */
  default Result<BulkSMSReceipt> sendBulk(BulkSMS bulkSMS) {
    throw new UnsupportedOperationException("Bulk SMS support not implemented by this adapter");
  }

  /**
   * Verify connection to SMS service.
   *
   * <p>Optional operation. Implementations may choose to return success immediately.
   *
   * @return Result with void or problem
   */
  default Result<Void> verify() {
    return Result.ok(null);
  }

  /**
   * Get delivery status for a message.
   *
   * <p>Optional operation. Not all providers support status tracking.
   *
   * @param messageId the message ID from receipt
   * @return Result with status or problem
   */
  default Result<SMSStatus> getStatus(String messageId) {
    throw new UnsupportedOperationException("Status tracking not implemented by this adapter");
  }

  /** Receipt returned after successful SMS send. */
  record SMSReceipt(String messageId, String status, PhoneNumber to) {
    public SMSReceipt {
      if (messageId == null || messageId.isBlank()) {
        throw new IllegalArgumentException("messageId must not be null or blank");
      }
      if (status == null || status.isBlank()) {
        throw new IllegalArgumentException("status must not be null or blank");
      }
      if (to == null) {
        throw new IllegalArgumentException("to phone number must not be null");
      }
    }

    public static SMSReceipt of(String messageId, String status, PhoneNumber to) {
      return new SMSReceipt(messageId, status, to);
    }
  }

  /** Receipt returned after successful bulk SMS send. */
  record BulkSMSReceipt(int totalMessages, int successCount, int failureCount) {
    public BulkSMSReceipt {
      if (totalMessages < 0) {
        throw new IllegalArgumentException("totalMessages must be non-negative");
      }
      if (successCount < 0) {
        throw new IllegalArgumentException("successCount must be non-negative");
      }
      if (failureCount < 0) {
        throw new IllegalArgumentException("failureCount must be non-negative");
      }
      if (successCount + failureCount != totalMessages) {
        throw new IllegalArgumentException("successCount + failureCount must equal totalMessages");
      }
    }

    public static BulkSMSReceipt of(int totalMessages, int successCount, int failureCount) {
      return new BulkSMSReceipt(totalMessages, successCount, failureCount);
    }
  }

  /** SMS delivery status. */
  enum SMSStatus {
    QUEUED,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    UNKNOWN
  }
}
