package com.marcusprado02.commons.app.webhooks;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility for generating and verifying webhook signatures.
 *
 * <p>Uses HMAC-SHA256 to sign webhook payloads, allowing receivers to verify that webhooks
 * originated from a trusted source.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Sender side
 * String signature = WebhookSignature.generate(payload, secret);
 * // Add to header: X-Webhook-Signature: sha256=<signature>
 *
 * // Receiver side
 * boolean valid = WebhookSignature.verify(payload, secret, receivedSignature);
 * }</pre>
 */
public final class WebhookSignature {

  private static final String HMAC_SHA256 = "HmacSHA256";
  private static final String SIGNATURE_PREFIX = "sha256=";

  private WebhookSignature() {}

  /**
   * Generates a signature for the given payload using the secret key.
   *
   * @param payload the payload to sign
   * @param secret the secret key
   * @return the signature in format "sha256=<hex>"
   */
  public static String generate(String payload, String secret) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      SecretKeySpec secretKeySpec =
          new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
      mac.init(secretKeySpec);

      byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      String hexHash = bytesToHex(hash);
      return SIGNATURE_PREFIX + hexHash;
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new WebhookSignatureException("Failed to generate signature", e);
    }
  }

  /**
   * Verifies that a signature matches the expected signature for the payload.
   *
   * @param payload the payload that was signed
   * @param secret the secret key
   * @param signature the signature to verify
   * @return true if valid
   */
  public static boolean verify(String payload, String secret, String signature) {
    String expected = generate(payload, secret);
    return constantTimeEquals(expected, signature);
  }

  /**
   * Constant-time string comparison to prevent timing attacks.
   *
   * @param a first string
   * @param b second string
   * @return true if equal
   */
  private static boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null || a.length() != b.length()) {
      return false;
    }

    byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
    byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

    int result = 0;
    for (int i = 0; i < aBytes.length; i++) {
      result |= aBytes[i] ^ bBytes[i];
    }
    return result == 0;
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  public static class WebhookSignatureException extends RuntimeException {
    public WebhookSignatureException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
