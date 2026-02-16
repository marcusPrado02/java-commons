package com.marcusprado02.commons.adapters.pdf.itext;

/**
 * Configuration for iText PDF adapter.
 *
 * <p>Controls PDF generation behavior including compression, PDF/A compliance, and metadata
 * settings.
 *
 * @param compressContent whether to compress PDF content streams
 * @param fullCompressionMode whether to use full compression mode
 * @param pdfVersion PDF version to generate (e.g., "1.7", "2.0")
 * @param userPassword user password for encryption (null for no encryption)
 * @param ownerPassword owner password for encryption
 * @param encryptionBits encryption strength (40, 128, or 256 bits)
 * @param bufferSize buffer size for output operations
 * @since 0.1.0
 */
public record ITextConfiguration(
    boolean compressContent,
    boolean fullCompressionMode,
    String pdfVersion,
    String userPassword,
    String ownerPassword,
    int encryptionBits,
    int bufferSize) {

  public ITextConfiguration {
    if (pdfVersion != null && !pdfVersion.matches("1\\.[4-7]|2\\.0")) {
      throw new IllegalArgumentException("Invalid PDF version: " + pdfVersion);
    }
    if (encryptionBits != 0
        && encryptionBits != 40
        && encryptionBits != 128
        && encryptionBits != 256) {
      throw new IllegalArgumentException("Encryption bits must be 0 (none), 40, 128, or 256");
    }
    if (bufferSize <= 0) {
      throw new IllegalArgumentException("Buffer size must be positive");
    }
  }

  /** Creates default configuration for standard PDF generation. */
  public static ITextConfiguration defaultConfig() {
    return new Builder().build();
  }

  /** Creates configuration optimized for file size (maximum compression). */
  public static ITextConfiguration compressed() {
    return new Builder().compressContent(true).fullCompressionMode(true).build();
  }

  /** Creates configuration for PDF 2.0 with modern features. */
  public static ITextConfiguration pdf20() {
    return new Builder().pdfVersion("2.0").compressContent(true).build();
  }

  /**
   * Creates configuration with 256-bit AES encryption.
   *
   * @param userPassword password for opening the document
   * @param ownerPassword password for changing permissions
   */
  public static ITextConfiguration encrypted(String userPassword, String ownerPassword) {
    return new Builder()
        .userPassword(userPassword)
        .ownerPassword(ownerPassword)
        .encryptionBits(256)
        .build();
  }

  /** Creates a new builder for ITextConfiguration. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ITextConfiguration. */
  public static class Builder {
    private boolean compressContent = true;
    private boolean fullCompressionMode = false;
    private String pdfVersion = "1.7";
    private String userPassword;
    private String ownerPassword;
    private int encryptionBits = 0;
    private int bufferSize = 8192;

    public Builder compressContent(boolean compressContent) {
      this.compressContent = compressContent;
      return this;
    }

    public Builder fullCompressionMode(boolean fullCompressionMode) {
      this.fullCompressionMode = fullCompressionMode;
      return this;
    }

    public Builder pdfVersion(String pdfVersion) {
      this.pdfVersion = pdfVersion;
      return this;
    }

    public Builder userPassword(String userPassword) {
      this.userPassword = userPassword;
      return this;
    }

    public Builder ownerPassword(String ownerPassword) {
      this.ownerPassword = ownerPassword;
      return this;
    }

    public Builder encryptionBits(int encryptionBits) {
      this.encryptionBits = encryptionBits;
      return this;
    }

    public Builder bufferSize(int bufferSize) {
      this.bufferSize = bufferSize;
      return this;
    }

    public ITextConfiguration build() {
      return new ITextConfiguration(
          compressContent,
          fullCompressionMode,
          pdfVersion,
          userPassword,
          ownerPassword,
          encryptionBits,
          bufferSize);
    }
  }
}
