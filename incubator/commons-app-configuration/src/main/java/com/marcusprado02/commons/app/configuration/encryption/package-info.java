/**
 * Configuration encryption support for protecting sensitive values.
 *
 * <p>Key components:
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.app.configuration.encryption.ConfigurationEncryptor} -
 *       Main encryption interface
 *   <li>{@link com.marcusprado02.commons.app.configuration.encryption.AesConfigurationEncryptor} -
 *       AES-GCM implementation
 *   <li>{@link com.marcusprado02.commons.app.configuration.encryption.NoOpConfigurationEncryptor} -
 *       No-op implementation for development
 * </ul>
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // Generate key
 * String key = AesConfigurationEncryptor.generateKey();
 *
 * // Create encryptor
 * ConfigurationEncryptor encryptor = new AesConfigurationEncryptor(key);
 *
 * // Encrypt value
 * String encrypted = encryptor.encrypt("secret-password");
 * System.out.println(encrypted);  // {cipher}AbCd...XyZ==
 *
 * // Decrypt value
 * String decrypted = encryptor.decrypt(encrypted);
 * System.out.println(decrypted);  // secret-password
 *
 * // Auto-decrypt if needed
 * String value = encryptor.decryptIfNeeded("{cipher}...");  // decrypts
 * String plain = encryptor.decryptIfNeeded("plain-text");   // returns as-is
 * }</pre>
 */
package com.marcusprado02.commons.app.configuration.encryption;
