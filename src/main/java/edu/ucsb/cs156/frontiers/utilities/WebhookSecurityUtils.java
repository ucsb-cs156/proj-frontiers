package edu.ucsb.cs156.frontiers.utilities;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/** Utility class for webhook security validation */
@Slf4j
public class WebhookSecurityUtils {

  private WebhookSecurityUtils() {
    // Utility Class
  }

  private static final String HMAC_SHA256 = "HmacSHA256";
  private static final Pattern GITHUB_SIGNATURE_PATTERN =
      Pattern.compile("^sha256=[a-fA-F0-9]{64}$");

  /**
   * Validates GitHub webhook signature using HMAC-SHA256
   *
   * @param payload the raw payload body as string
   * @param signature the GitHub signature header (e.g., "sha256=abc123...")
   * @param secret the webhook secret
   * @return true if signature is valid, false otherwise
   */
  public static boolean validateGitHubSignature(String payload, String signature, String secret)
      throws NoSuchAlgorithmException, InvalidKeyException {
    if (payload == null || signature == null || secret == null) {
      log.warn("Null values provided for webhook validation");
      return false;
    }

    if (!GITHUB_SIGNATURE_PATTERN.matcher(signature).matches()) {
      log.warn("Invalid signature format");
      return false;
    }

    String expectedSignature = "sha256=" + calculateHmacSha256(payload, secret);
    boolean isValid = safeEquals(signature, expectedSignature);

    if (!isValid) {
      log.warn("Webhook signature validation failed");
      return false;
    }

    return true;
  }

  /**
   * Calculates HMAC-SHA256 signature for given payload and secret
   *
   * @param payload the payload to sign
   * @param secret the secret key
   * @return hex-encoded signature
   */
  private static String calculateHmacSha256(String payload, String secret)
      throws NoSuchAlgorithmException, InvalidKeyException {
    Mac mac = Mac.getInstance(HMAC_SHA256);
    SecretKeySpec secretKeySpec =
        new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
    mac.init(secretKeySpec);
    byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(signature);
  }

  /**
   * Converts byte array to hexadecimal string
   *
   * @param bytes the byte array
   * @return hex string
   */
  private static String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  /**
   * Constant-time string comparison to prevent timing attacks
   *
   * @param a first string
   * @param b second string
   * @return true if strings are equal
   */
  private static boolean safeEquals(String a, String b) {
    return MessageDigest.isEqual(
        a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
  }
}
