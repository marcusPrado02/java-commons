package com.marcusprado02.commons.ports.sms;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class SmsModelTest {

  private static final PhoneNumber FROM = PhoneNumber.of("+15551234567");
  private static final PhoneNumber TO = PhoneNumber.of("+15559876543");

  // --- SMSOptions ---

  @Test
  void smsOptions_defaults_are_valid() {
    SMSOptions opts = SMSOptions.defaults();
    assertFalse(opts.deliveryReceipt());
    assertEquals(1440, opts.validityPeriodMinutes());
    assertEquals(SMSOptions.SMSPriority.NORMAL, opts.priority());
    assertNull(opts.webhookUrl());
  }

  @Test
  void smsOptions_builder_sets_all_fields() {
    SMSOptions opts =
        SMSOptions.builder()
            .deliveryReceipt(true)
            .validityPeriodMinutes(60)
            .priority(SMSOptions.SMSPriority.HIGH)
            .webhookUrl("https://example.com/webhook")
            .build();
    assertTrue(opts.deliveryReceipt());
    assertEquals(60, opts.validityPeriodMinutes());
    assertEquals(SMSOptions.SMSPriority.HIGH, opts.priority());
    assertEquals("https://example.com/webhook", opts.webhookUrl());
  }

  @Test
  void smsOptions_builder_from_existing() {
    SMSOptions existing = SMSOptions.defaults();
    SMSOptions updated = SMSOptions.builder(existing).deliveryReceipt(true).build();
    assertTrue(updated.deliveryReceipt());
    assertEquals(existing.validityPeriodMinutes(), updated.validityPeriodMinutes());
  }

  @Test
  void smsOptions_negative_validity_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SMSOptions.builder().validityPeriodMinutes(-1).build());
  }

  @Test
  void smsOptions_validity_exceeds_max_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SMSOptions.builder().validityPeriodMinutes(10081).build());
  }

  @Test
  void smsOptions_validity_at_max_boundary_is_valid() {
    SMSOptions opts = SMSOptions.builder().validityPeriodMinutes(10080).build();
    assertEquals(10080, opts.validityPeriodMinutes());
  }

  @Test
  void smsOptions_priority_values_all_exist() {
    assertEquals(4, SMSOptions.SMSPriority.values().length);
  }

  // --- SMS ---

  @Test
  void sms_of_string_creates_valid_sms() {
    SMS sms = SMS.of("+15551234567", "+15559876543", "Hello World");
    assertEquals(FROM, sms.from());
    assertEquals(TO, sms.to());
    assertEquals("Hello World", sms.message());
    assertNotNull(sms.options());
  }

  @Test
  void sms_of_phone_number_creates_valid_sms() {
    SMS sms = SMS.of(FROM, TO, "Test message");
    assertEquals(FROM, sms.from());
    assertEquals(TO, sms.to());
  }

  @Test
  void sms_builder_with_delivery_receipt() {
    SMS sms = SMS.builder().from(FROM).to(TO).message("msg").withDeliveryReceipt().build();
    assertTrue(sms.options().deliveryReceipt());
  }

  @Test
  void sms_builder_with_validity_period() {
    SMS sms = SMS.builder().from(FROM).to(TO).message("msg").validityPeriod(60).build();
    assertEquals(60, sms.options().validityPeriodMinutes());
  }

  @Test
  void sms_null_from_throws() {
    assertThrows(NullPointerException.class, () -> new SMS(null, TO, "msg", SMSOptions.defaults()));
  }

  @Test
  void sms_null_to_throws() {
    assertThrows(
        NullPointerException.class, () -> new SMS(FROM, null, "msg", SMSOptions.defaults()));
  }

  @Test
  void sms_null_message_throws() {
    assertThrows(NullPointerException.class, () -> new SMS(FROM, TO, null, SMSOptions.defaults()));
  }

  @Test
  void sms_null_options_throws() {
    assertThrows(NullPointerException.class, () -> new SMS(FROM, TO, "msg", null));
  }

  @Test
  void sms_blank_message_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> new SMS(FROM, TO, "  ", SMSOptions.defaults()));
  }

  @Test
  void sms_message_too_long_throws() {
    String longMsg = "A".repeat(1601);
    assertThrows(
        IllegalArgumentException.class, () -> new SMS(FROM, TO, longMsg, SMSOptions.defaults()));
  }

  @Test
  void sms_message_at_max_length_is_valid() {
    String maxMsg = "A".repeat(1600);
    SMS sms = new SMS(FROM, TO, maxMsg, SMSOptions.defaults());
    assertEquals(1600, sms.message().length());
  }

  // --- BulkSMS ---

  @Test
  void bulkSms_of_string_creates_valid() {
    BulkSMS bulk = BulkSMS.of("+15551234567", List.of("+15559876543", "+15550001111"), "Hello");
    assertNotNull(bulk);
    assertEquals(2, bulk.to().size());
  }

  @Test
  void bulkSms_null_from_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new BulkSMS(null, List.of(TO), "msg", SMSOptions.defaults()));
  }

  @Test
  void bulkSms_null_to_throws() {
    assertThrows(
        NullPointerException.class, () -> new BulkSMS(FROM, null, "msg", SMSOptions.defaults()));
  }

  @Test
  void bulkSms_null_message_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new BulkSMS(FROM, List.of(TO), null, SMSOptions.defaults()));
  }

  @Test
  void bulkSms_null_options_throws() {
    assertThrows(NullPointerException.class, () -> new BulkSMS(FROM, List.of(TO), "msg", null));
  }

  @Test
  void bulkSms_empty_recipients_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BulkSMS(FROM, List.of(), "msg", SMSOptions.defaults()));
  }

  @Test
  void bulkSms_blank_message_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BulkSMS(FROM, List.of(TO), "  ", SMSOptions.defaults()));
  }

  @Test
  void bulkSms_message_too_long_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BulkSMS(FROM, List.of(TO), "A".repeat(1601), SMSOptions.defaults()));
  }

  @Test
  void bulkSms_too_many_recipients_throws() {
    List<PhoneNumber> tooMany = java.util.stream.Stream.generate(() -> TO).limit(1001).toList();
    assertThrows(
        IllegalArgumentException.class,
        () -> new BulkSMS(FROM, tooMany, "msg", SMSOptions.defaults()));
  }

  @Test
  void bulkSms_builder_to_all_strings() {
    BulkSMS bulk =
        BulkSMS.builder()
            .from(FROM)
            .toAll(List.of("+15551234567", "+15559876543"))
            .message("msg")
            .withDeliveryReceipt()
            .build();
    assertEquals(2, bulk.to().size());
    assertTrue(bulk.options().deliveryReceipt());
  }

  @Test
  void bulkSms_builder_to_all_phones() {
    BulkSMS bulk =
        BulkSMS.builder()
            .from(FROM)
            .to(TO)
            .toAllPhones(List.of(PhoneNumber.of("+15550001111")))
            .message("msg")
            .validityPeriod(120)
            .build();
    assertEquals(2, bulk.to().size());
  }

  // --- MMS ---

  @Test
  void mms_with_text_and_media_is_valid() {
    MMS.MediaContent media = MMS.MediaContent.image(new byte[] {1, 2, 3}, "jpeg");
    MMS mms = MMS.builder().from(FROM).to(TO).message("Caption").addMedia(media).build();
    assertNotNull(mms);
    assertEquals(1, mms.mediaContents().size());
    assertEquals("Caption", mms.message());
  }

  @Test
  void mms_text_only_is_valid() {
    MMS mms = MMS.builder().from(FROM).to(TO).message("Text only MMS").build();
    assertTrue(mms.mediaContents().isEmpty());
    assertEquals("Text only MMS", mms.message());
  }

  @Test
  void mms_null_from_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new MMS(null, TO, null, List.of(), SMSOptions.defaults()));
  }

  @Test
  void mms_null_to_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new MMS(FROM, null, null, List.of(), SMSOptions.defaults()));
  }

  @Test
  void mms_null_media_contents_throws() {
    assertThrows(
        NullPointerException.class, () -> new MMS(FROM, TO, "msg", null, SMSOptions.defaults()));
  }

  @Test
  void mms_null_options_throws() {
    assertThrows(NullPointerException.class, () -> new MMS(FROM, TO, "msg", List.of(), null));
  }

  @Test
  void mms_empty_media_and_null_message_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MMS(FROM, TO, null, List.of(), SMSOptions.defaults()));
  }

  @Test
  void mms_empty_media_and_blank_message_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MMS(FROM, TO, "  ", List.of(), SMSOptions.defaults()));
  }

  @Test
  void mms_media_too_large_throws() {
    byte[] bigData = new byte[1048577]; // 1MB + 1 byte
    MMS.MediaContent media = new MMS.MediaContent(bigData, "image/jpeg", null);
    assertThrows(
        IllegalArgumentException.class,
        () -> new MMS(FROM, TO, null, List.of(media), SMSOptions.defaults()));
  }

  @Test
  void mms_builder_add_image_video_audio() {
    MMS mms =
        MMS.builder()
            .from(FROM)
            .to(TO)
            .addImage(new byte[] {1}, "jpeg")
            .addVideo(new byte[] {2}, "mp4")
            .addAudio(new byte[] {3}, "mp3")
            .build();
    assertEquals(3, mms.mediaContents().size());
  }

  @Test
  void mediaContent_null_content_throws() {
    assertThrows(NullPointerException.class, () -> new MMS.MediaContent(null, "image/jpeg", null));
  }

  @Test
  void mediaContent_null_content_type_throws() {
    assertThrows(
        NullPointerException.class, () -> new MMS.MediaContent(new byte[] {1}, null, null));
  }

  @Test
  void mediaContent_empty_content_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MMS.MediaContent(new byte[0], "image/jpeg", null));
  }

  @Test
  void mediaContent_blank_content_type_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> new MMS.MediaContent(new byte[] {1}, "  ", null));
  }

  @Test
  void mediaContent_invalid_content_type_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MMS.MediaContent(new byte[] {1}, "application/octet-stream", null));
  }

  @Test
  void mediaContent_pdf_is_valid() {
    MMS.MediaContent mc = new MMS.MediaContent(new byte[] {1}, "application/pdf", "doc.pdf");
    assertEquals("application/pdf", mc.contentType());
    assertEquals("doc.pdf", mc.filename());
  }

  @Test
  void mediaContent_text_plain_is_valid() {
    MMS.MediaContent mc = new MMS.MediaContent(new byte[] {1}, "text/plain", null);
    assertEquals("text/plain", mc.contentType());
  }
}
