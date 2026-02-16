package com.marcusprado02.commons.app.configuration.encryption;

/**
 * No-op encryptor that doesn't encrypt or decrypt anything.
 *
 * <p>Useful for development or when encryption is not required.
 *
 * @see ConfigurationEncryptor
 */
public class NoOpConfigurationEncryptor implements ConfigurationEncryptor {

  @Override
  public String encrypt(String plainText) {
    return plainText;
  }

  @Override
  public String decrypt(String encryptedText) {
    return encryptedText;
  }

  @Override
  public boolean isEncrypted(String value) {
    return false;
  }
}
