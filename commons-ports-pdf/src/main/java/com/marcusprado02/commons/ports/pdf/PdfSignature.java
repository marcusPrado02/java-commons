package com.marcusprado02.commons.ports.pdf;

import java.io.InputStream;
import java.time.Instant;

/**
 * Configuration for digitally signing PDF documents.
 *
 * <p>Supports PKCS#12 keystores for digital signatures with optional visible signature fields.
 *
 * @param keystoreData keystore file data (PKCS#12 format)
 * @param keystorePassword password to access the keystore
 * @param keyAlias alias of the private key in keystore
 * @param reason reason for signing the document
 * @param location location where signing occurred
 * @param contactInfo contact information of the signer
 * @param signatureField visible signature field configuration
 * @param timestamp timestamp for the signature
 * @since 0.1.0
 */
public record PdfSignature(
    InputStream keystoreData,
    String keystorePassword,
    String keyAlias,
    String reason,
    String location,
    String contactInfo,
    SignatureField signatureField,
    Instant timestamp) {

  public PdfSignature {
    if (keystoreData == null) {
      throw new IllegalArgumentException("Keystore data is required");
    }
    if (keystorePassword == null || keystorePassword.isBlank()) {
      throw new IllegalArgumentException("Keystore password is required");
    }
    if (keyAlias == null || keyAlias.isBlank()) {
      throw new IllegalArgumentException("Key alias is required");
    }
    timestamp = timestamp == null ? Instant.now() : timestamp;
  }

  /**
   * Visible signature field configuration.
   *
   * @param page page number (1-based) where signature appears
   * @param x x-coordinate in points from left
   * @param y y-coordinate in points from bottom
   * @param width field width in points
   * @param height field height in points
   * @param imageData optional signature image
   */
  public record SignatureField(
      int page, float x, float y, float width, float height, InputStream imageData) {

    public SignatureField {
      if (page < 1) {
        throw new IllegalArgumentException("Page number must be >= 1");
      }
      if (width <= 0 || height <= 0) {
        throw new IllegalArgumentException("Width and height must be positive");
      }
    }

    public SignatureField(int page, float x, float y, float width, float height) {
      this(page, x, y, width, height, null);
    }
  }

  /** Creates a new builder for PdfSignature. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for PdfSignature. */
  public static class Builder {
    private InputStream keystoreData;
    private String keystorePassword;
    private String keyAlias;
    private String reason;
    private String location;
    private String contactInfo;
    private SignatureField signatureField;
    private Instant timestamp;

    public Builder keystoreData(InputStream keystoreData) {
      this.keystoreData = keystoreData;
      return this;
    }

    public Builder keystorePassword(String keystorePassword) {
      this.keystorePassword = keystorePassword;
      return this;
    }

    public Builder keyAlias(String keyAlias) {
      this.keyAlias = keyAlias;
      return this;
    }

    public Builder reason(String reason) {
      this.reason = reason;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    public Builder contactInfo(String contactInfo) {
      this.contactInfo = contactInfo;
      return this;
    }

    public Builder signatureField(SignatureField signatureField) {
      this.signatureField = signatureField;
      return this;
    }

    public Builder signatureField(int page, float x, float y, float width, float height) {
      this.signatureField = new SignatureField(page, x, y, width, height);
      return this;
    }

    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public PdfSignature build() {
      return new PdfSignature(
          keystoreData,
          keystorePassword,
          keyAlias,
          reason,
          location,
          contactInfo,
          signatureField,
          timestamp);
    }
  }
}
