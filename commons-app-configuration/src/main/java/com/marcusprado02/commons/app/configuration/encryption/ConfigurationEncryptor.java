package com.marcusprado02.commons.app.configuration.encryption;

/**
 * Interface for encrypting and decrypting configuration values.
 *
 * <p>Used to protect sensitive configuration properties like passwords, API keys, and secrets.
 *
 * <p>Implementations should be thread-safe.
 *
 * @see AesConfigurationEncryptor
 * @see NoOpConfigurationEncryptor
 */
public interface ConfigurationEncryptor {

  /**
   * Encrypts a configuration value.
   *
   * @param plainText the plain text value to encrypt
   * @return the encrypted value
   * @throws EncryptionException if encryption fails
   */
  String encrypt(String plainText) throws EncryptionException;

  /**
   * Decrypts an encrypted configuration value.
   *
   * @param encryptedText the encrypted value to decrypt
   * @return the decrypted plain text value
   * @throws EncryptionException if decryption fails
   */
  String decrypt(String encryptedText) throws EncryptionException;

  /**
   * Checks if a value appears to be encrypted.
   *
   * <p>Typically checks for a prefix like {cipher} or ENC().
   *
   * @param value the value to check
   * @return true if the value appears to be encrypted
   */
  boolean isEncrypted(String value);

  /**
   * Decrypts a value only if it's encrypted, otherwise returns it unchanged.
   *
   * @param value the value to decrypt
   * @return the decrypted value if encrypted, otherwise the original value
   */
  default String decryptIfNeeded(String value) {
    if (value == null) return null;
    if (isEncrypted(value)) {
      try {
        return decrypt(value);
      } catch (EncryptionException e) {
        throw new RuntimeException("Failed to decrypt value", e);
      }
    }
    return value;
  }
}
