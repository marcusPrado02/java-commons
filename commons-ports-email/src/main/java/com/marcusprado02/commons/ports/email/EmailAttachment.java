package com.marcusprado02.commons.ports.email;

import java.io.InputStream;
import java.util.Objects;

/**
 * Email attachment value object.
 *
 * <p>Represents a file attachment to be included in an email.
 */
public record EmailAttachment(String filename, String contentType, byte[] data) {

  public EmailAttachment {
    Objects.requireNonNull(filename, "filename must not be null");
    Objects.requireNonNull(contentType, "contentType must not be null");
    Objects.requireNonNull(data, "data must not be null");

    if (filename.trim().isEmpty()) {
      throw new IllegalArgumentException("filename must not be blank");
    }
    if (contentType.trim().isEmpty()) {
      throw new IllegalArgumentException("contentType must not be blank");
    }
    if (data.length == 0) {
      throw new IllegalArgumentException("attachment data must not be empty");
    }
  }

  /**
   * Creates an attachment with the given parameters.
   *
   * @param filename the filename
   * @param contentType the MIME content type
   * @param data the file data
   * @return EmailAttachment
   */
  public static EmailAttachment of(String filename, String contentType, byte[] data) {
    return new EmailAttachment(filename, contentType, data);
  }

  /**
   * Creates an attachment from an InputStream.
   *
   * @param filename the filename
   * @param contentType the MIME content type
   * @param inputStream the input stream containing file data
   * @return EmailAttachment
   * @throws RuntimeException if reading from stream fails
   */
  public static EmailAttachment fromStream(
      String filename, String contentType, InputStream inputStream) {
    try {
      byte[] data = inputStream.readAllBytes();
      return new EmailAttachment(filename, contentType, data);
    } catch (Exception e) {
      throw new RuntimeException("Failed to read attachment from stream", e);
    }
  }

  /**
   * Returns the size of the attachment in bytes.
   *
   * @return size in bytes
   */
  public long size() {
    return data.length;
  }
}
