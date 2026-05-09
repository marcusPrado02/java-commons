package com.marcusprado02.commons.ports.sms;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.kernel.result.Result;
import org.junit.jupiter.api.Test;

class SmsPortBranchTest {

  // --- SMSReceipt ---

  @Test
  void smsReceipt_valid() {
    PhoneNumber to = PhoneNumber.of("+15551234567");
    SMSPort.SMSReceipt r = SMSPort.SMSReceipt.of("msg-1", "sent", to);
    assertEquals("msg-1", r.messageId());
    assertEquals("sent", r.status());
    assertEquals(to, r.to());
  }

  @Test
  void smsReceipt_null_messageId_throws() {
    PhoneNumber to = PhoneNumber.of("+15551234567");
    assertThrows(IllegalArgumentException.class, () -> SMSPort.SMSReceipt.of(null, "sent", to));
  }

  @Test
  void smsReceipt_blank_messageId_throws() {
    PhoneNumber to = PhoneNumber.of("+15551234567");
    assertThrows(IllegalArgumentException.class, () -> SMSPort.SMSReceipt.of("  ", "sent", to));
  }

  @Test
  void smsReceipt_null_status_throws() {
    PhoneNumber to = PhoneNumber.of("+15551234567");
    assertThrows(IllegalArgumentException.class, () -> SMSPort.SMSReceipt.of("msg-1", null, to));
  }

  @Test
  void smsReceipt_blank_status_throws() {
    PhoneNumber to = PhoneNumber.of("+15551234567");
    assertThrows(IllegalArgumentException.class, () -> SMSPort.SMSReceipt.of("msg-1", "  ", to));
  }

  @Test
  void smsReceipt_null_to_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> SMSPort.SMSReceipt.of("msg-1", "sent", null));
  }

  // --- BulkSMSReceipt ---

  @Test
  void bulkSmsReceipt_valid() {
    SMSPort.BulkSMSReceipt r = SMSPort.BulkSMSReceipt.of(10, 8, 2);
    assertEquals(10, r.totalMessages());
    assertEquals(8, r.successCount());
    assertEquals(2, r.failureCount());
  }

  @Test
  void bulkSmsReceipt_negative_total_throws() {
    assertThrows(IllegalArgumentException.class, () -> SMSPort.BulkSMSReceipt.of(-1, 0, 0));
  }

  @Test
  void bulkSmsReceipt_negative_success_throws() {
    assertThrows(IllegalArgumentException.class, () -> SMSPort.BulkSMSReceipt.of(1, -1, 0));
  }

  @Test
  void bulkSmsReceipt_negative_failure_throws() {
    assertThrows(IllegalArgumentException.class, () -> SMSPort.BulkSMSReceipt.of(1, 0, -1));
  }

  @Test
  void bulkSmsReceipt_counts_mismatch_throws() {
    assertThrows(IllegalArgumentException.class, () -> SMSPort.BulkSMSReceipt.of(10, 3, 3));
  }

  // --- SMSPort default methods ---

  @Test
  void smsPort_verify_returns_ok() {
    SMSPort port = sms -> Result.ok(null);
    Result<Void> result = port.verify();
    assertTrue(result.isOk());
  }

  @Test
  void smsPort_sendMMS_throws_unsupported() {
    SMSPort port = sms -> Result.ok(null);
    MMS mms =
        MMS.builder()
            .from(PhoneNumber.of("+15551234567"))
            .to(PhoneNumber.of("+15559876543"))
            .message("test")
            .build();
    assertThrows(UnsupportedOperationException.class, () -> port.sendMMS(mms));
  }

  @Test
  void smsPort_sendBulk_throws_unsupported() {
    SMSPort port = sms -> Result.ok(null);
    BulkSMS bulk =
        BulkSMS.builder()
            .from(PhoneNumber.of("+15551234567"))
            .to("+15559876543")
            .message("hello")
            .build();
    assertThrows(UnsupportedOperationException.class, () -> port.sendBulk(bulk));
  }

  @Test
  void smsPort_getStatus_throws_unsupported() {
    SMSPort port = sms -> Result.ok(null);
    assertThrows(UnsupportedOperationException.class, () -> port.getStatus("msg-1"));
  }

  // --- SMSStatus enum ---

  @Test
  void smsStatus_all_values() {
    assertEquals(6, SMSPort.SMSStatus.values().length);
    assertNotNull(SMSPort.SMSStatus.DELIVERED);
    assertNotNull(SMSPort.SMSStatus.UNKNOWN);
  }
}
