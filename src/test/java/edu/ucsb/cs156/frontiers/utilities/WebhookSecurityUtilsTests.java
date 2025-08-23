package edu.ucsb.cs156.frontiers.utilities;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

public class WebhookSecurityUtilsTests {

  private static final String TEST_SECRET = "test_webhook_secret_123";

  // Helper method to generate valid signatures for testing
  private String generateValidSignature(String payload, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKeySpec =
        new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(secretKeySpec);
    byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    return "sha256=" + bytesToHex(signature);
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  @Test
  public void testValidateGitHubSignature_validSignature() throws Exception {
    String payload = "{\"action\":\"opened\"}";
    String signature = generateValidSignature(payload, TEST_SECRET);

    assertTrue(WebhookSecurityUtils.validateGitHubSignature(payload, signature, TEST_SECRET));
  }

  @Test
  public void testValidateGitHubSignature_invalidSignature() throws Exception {
    String payload = "{\"action\":\"opened\"}";
    String wrongSignature = "sha256=wrongsignature";

    assertFalse(WebhookSecurityUtils.validateGitHubSignature(payload, wrongSignature, TEST_SECRET));
  }

  @Test
  public void testValidateGitHubSignature_nullPayload() throws Exception {
    String signature = generateValidSignature("{\"action\":\"opened\"}", TEST_SECRET);

    assertFalse(WebhookSecurityUtils.validateGitHubSignature(null, signature, TEST_SECRET));
  }

  @Test
  public void testValidateGitHubSignature_nullSignature() throws Exception {
    String payload = "{\"action\":\"opened\"}";

    assertFalse(WebhookSecurityUtils.validateGitHubSignature(payload, null, TEST_SECRET));
  }

  @Test
  public void testValidateGitHubSignature_nullSecret() throws Exception {
    String payload = "{\"action\":\"opened\"}";
    String signature = generateValidSignature(payload, TEST_SECRET);

    assertFalse(WebhookSecurityUtils.validateGitHubSignature(payload, signature, null));
  }

  @Test
  public void testValidateGitHubSignature_invalidSignatureFormat() throws Exception {
    String payload = "{\"action\":\"opened\"}";
    String invalidSignature =
        "md5=d57c68ca6f92289e6987922ff26938930f6e66a2d161ef06abdf1859230aa23c";

    assertFalse(
        WebhookSecurityUtils.validateGitHubSignature(payload, invalidSignature, TEST_SECRET));
  }

  @Test
  public void testValidateGitHubSignature_emptyPayload() throws Exception {
    String payload = "";
    String signature = generateValidSignature(payload, TEST_SECRET);

    assertTrue(WebhookSecurityUtils.validateGitHubSignature(payload, signature, TEST_SECRET));
  }

  @Test
  public void testValidateGitHubSignature_differentSecret() throws Exception {
    String payload = "{\"action\":\"opened\"}";
    String differentSecret = "different_secret";
    String signature = generateValidSignature(payload, differentSecret);

    assertFalse(WebhookSecurityUtils.validateGitHubSignature(payload, signature, TEST_SECRET));
  }

  @Test
  public void testValidateGitHubSignature_signatureWithoutPrefix() throws Exception {
    String payload = "{\"action\":\"opened\"}";
    String validSig = generateValidSignature(payload, TEST_SECRET);
    String signatureWithoutPrefix = validSig.substring(7); // Remove "sha256=" prefix

    assertFalse(
        WebhookSecurityUtils.validateGitHubSignature(payload, signatureWithoutPrefix, TEST_SECRET));
  }
}
