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
public record ItextConfiguration(
    boolean compressContent,
    boolean fullCompressionMode,
    String pdfVersion,
    String userPassword,
    String ownerPassword,
    int encryptionBits,
    int bufferSize) {

  /** Validates the configuration parameters. */
  public ItextConfiguration {
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
  public static ItextConfiguration defaultConfig() {
    return new Builder().build();
  }

  /** Creates configuration optimized for file size (maximum compression). */
  public static ItextConfiguration compressed() {
    return new Builder().compressContent(true).fullCompressionMode(true).build();
  }

  /** Creates configuration for PDF 2.0 with modern features. */
  public static ItextConfiguration pdf20() {
    return new Builder().pdfVersion("2.0").compressContent(true).build();
  }

  /**
   * Creates configuration with 256-bit AES encryption.
   *
   * @param userPassword password for opening the document
   * @param ownerPassword password for changing permissions
   * @return encrypted configuration
   */
  public static ItextConfiguration encrypted(String userPassword, String ownerPassword) {
    return new Builder()
        .userPassword(userPassword)
        .ownerPassword(ownerPassword)
        .encryptionBits(256)
        .build();
  }

  /**
   * Creates a new builder for ItextConfiguration.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ItextConfiguration. */
  public static class Builder {
    private boolean compressContent = true;
    private boolean fullCompressionMode = false;
    private String pdfVersion = "1.7";
    private String userPassword;
    private String ownerPassword;
    private int encryptionBits = 0;
    private int bufferSize = 8192;

    /**
     * Sets whether to compress content.
     *
     * @param compressContent true to enable compression
     * @return this builder
     */
    public Builder compressContent(boolean compressContent) {
      this.compressContent = compressContent;
      return this;
    }

    /**
     * Sets whether to use full compression mode.
     *
     * @param fullCompressionMode true to enable full compression
     * @return this builder
     */
    public Builder fullCompressionMode(boolean fullCompressionMode) {
      this.fullCompressionMode = fullCompressionMode;
      return this;
    }

    /**
     * Sets the PDF version.
     *
     * @param pdfVersion the version string (e.g., "1.7")
     * @return this builder
     */
    public Builder pdfVersion(String pdfVersion) {
      this.pdfVersion = pdfVersion;
      return this;
    }

    /**
     * Sets the user password.
     *
     * @param userPassword password for opening the document
     * @return this builder
     */
    public Builder userPassword(String userPassword) {
      this.userPassword = userPassword;
      return this;
    }

    /**
     * Sets the owner password.
     *
     * @param ownerPassword password for changing permissions
     * @return this builder
     */
    public Builder ownerPassword(String ownerPassword) {
      this.ownerPassword = ownerPassword;
      return this;
    }

    /**
     * Sets the encryption bits.
     *
     * @param encryptionBits 0, 40, 128, or 256
     * @return this builder
     */
    public Builder encryptionBits(int encryptionBits) {
      this.encryptionBits = encryptionBits;
      return this;
    }

    /**
     * Sets the buffer size.
     *
     * @param bufferSize positive buffer size in bytes
     * @return this builder
     */
    public Builder bufferSize(int bufferSize) {
      this.bufferSize = bufferSize;
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return new ItextConfiguration
     */
    public ItextConfiguration build() {
      return new ItextConfiguration(
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
