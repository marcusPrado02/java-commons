package com.marcusprado02.commons.app.configuration.encryption;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-GCM based configuration encryptor.
 *
 * <p>Uses AES-256-GCM for authenticated encryption with associated data (AEAD).
 *
 * <p>Encrypted values are prefixed with {cipher} to indicate they need decryption.
 *
 * <p>Format: {cipher}base64(iv + ciphertext + tag)
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Generate a key
 * String key = AesConfigurationEncryptor.generateKey();
 *
 * // Create encryptor
 * ConfigurationEncryptor encryptor = new AesConfigurationEncryptor(key);
 *
 * // Encrypt sensitive value
 * String encrypted = encryptor.encrypt("my-secret-password");
 * // Result: {cipher}AbCd...XyZ==
 *
 * // Decrypt
 * String decrypted = encryptor.decrypt(encrypted);
 * // Result: my-secret-password
 * }</pre>
 *
 * @see ConfigurationEncryptor
 */
public class AesConfigurationEncryptor implements ConfigurationEncryptor {

  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;
  private static final String ENCRYPTED_PREFIX = "{cipher}";

  private final SecretKey secretKey;
  private final SecureRandom secureRandom;

  /**
   * Creates an AES encryptor with the given base64-encoded key.
   *
   * @param base64Key the AES key in base64 format
   */
  public AesConfigurationEncryptor(String base64Key) {
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    this.secureRandom = new SecureRandom();
  }

  /**
   * Generates a new AES-256 key.
   *
   * @return base64-encoded AES key
   */
  public static String generateKey() {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
      keyGenerator.init(256);
      SecretKey key = keyGenerator.generateKey();
      return Base64.getEncoder().encodeToString(key.getEncoded());
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate AES key", e);
    }
  }

  @Override
  public String encrypt(String plainText) throws EncryptionException {
    if (plainText == null) return null;

    try {
      // Generate random IV
      byte[] iv = new byte[GCM_IV_LENGTH];
      secureRandom.nextBytes(iv);

      // Initialize cipher
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

      // Encrypt
      byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

      // Combine IV + ciphertext
      ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
      buffer.put(iv);
      buffer.put(cipherText);

      // Encode to base64 with prefix
      return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(buffer.array());

    } catch (Exception e) {
      throw new EncryptionException("Encryption failed", e);
    }
  }

  @Override
  public String decrypt(String encryptedText) throws EncryptionException {
    if (encryptedText == null) return null;
    if (!isEncrypted(encryptedText)) {
      throw new EncryptionException("Value is not encrypted (missing {cipher} prefix)");
    }

    try {
      // Remove prefix and decode from base64
      String base64 = encryptedText.substring(ENCRYPTED_PREFIX.length());
      byte[] combined = Base64.getDecoder().decode(base64);

      // Extract IV and ciphertext
      ByteBuffer buffer = ByteBuffer.wrap(combined);
      byte[] iv = new byte[GCM_IV_LENGTH];
      buffer.get(iv);
      byte[] cipherText = new byte[buffer.remaining()];
      buffer.get(cipherText);

      // Initialize cipher
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

      // Decrypt
      byte[] plainText = cipher.doFinal(cipherText);
      return new String(plainText, StandardCharsets.UTF_8);

    } catch (Exception e) {
      throw new EncryptionException("Decryption failed", e);
    }
  }

  @Override
  public boolean isEncrypted(String value) {
    return value != null && value.startsWith(ENCRYPTED_PREFIX);
  }
}
