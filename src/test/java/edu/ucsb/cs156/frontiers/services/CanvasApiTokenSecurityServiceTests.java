package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import org.junit.jupiter.api.Test;

public class CanvasApiTokenSecurityServiceTests {

  private static final String VALID_KEY =
      Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

  @Test
  public void encrypt_thenDecrypt_roundTripsToken() {
    CanvasApiTokenSecurityService service = new CanvasApiTokenSecurityService(VALID_KEY);

    String encrypted = service.encrypt("test-api-token");

    assertNotEquals("test-api-token", encrypted);
    assertTrue(encrypted.startsWith("enc:v1:"));
    assertEquals("test-api-token", service.decrypt(encrypted));
  }

  @Test
  public void decrypt_plaintextLegacyValue_returnsOriginalValue() {
    CanvasApiTokenSecurityService service = new CanvasApiTokenSecurityService("");

    assertEquals("legacy-plaintext-token", service.decrypt("legacy-plaintext-token"));
  }

  @Test
  public void encrypt_withoutConfiguredKey_throwsIllegalStateException() {
    CanvasApiTokenSecurityService service = new CanvasApiTokenSecurityService("");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> service.encrypt("test-api-token"));
    assertEquals("Canvas API token encryption key is not configured", exception.getMessage());
  }
}
