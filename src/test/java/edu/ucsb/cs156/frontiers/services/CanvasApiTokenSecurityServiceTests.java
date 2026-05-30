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

  @Test
  public void encrypt_nullOrEmptyOrAlreadyEncrypted_returnsInputUnchanged() {
    CanvasApiTokenSecurityService service = new CanvasApiTokenSecurityService(VALID_KEY);

    assertEquals(null, service.encrypt(null));
    assertEquals("", service.encrypt(""));
    assertEquals("enc:v1:already-encrypted", service.encrypt("enc:v1:already-encrypted"));
  }

  @Test
  public void decrypt_nullOrEmpty_returnsInputUnchanged() {
    CanvasApiTokenSecurityService service = new CanvasApiTokenSecurityService(VALID_KEY);

    assertEquals(null, service.decrypt(null));
    assertEquals("", service.decrypt(""));
  }

  @Test
  public void decrypt_ivOnlyPayload_throwsMalformedException() {
    CanvasApiTokenSecurityService service = new CanvasApiTokenSecurityService(VALID_KEY);
    String ivOnlyPayload = Base64.getEncoder().encodeToString(new byte[12]);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> service.decrypt("enc:v1:" + ivOnlyPayload));
    assertEquals("Encrypted Canvas API token is malformed", exception.getMessage());
  }

  @Test
  public void encrypt_samePlaintextTwice_usesDifferentRandomIv() {
    CanvasApiTokenSecurityService service = new CanvasApiTokenSecurityService(VALID_KEY);

    String encrypted1 = service.encrypt("same-token");
    String encrypted2 = service.encrypt("same-token");

    assertNotEquals(encrypted1, encrypted2);
  }

  @Test
  public void encrypt_withNonBase64Key_throwsIllegalStateException() {
    CanvasApiTokenSecurityService service = new CanvasApiTokenSecurityService("%%%");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> service.encrypt("test-api-token"));
    assertEquals("Canvas API token encryption key must be Base64 encoded", exception.getMessage());
  }

  @Test
  public void encrypt_withInvalidDecodedKeyLength_throwsIllegalStateException() {
    String invalidLengthKey = Base64.getEncoder().encodeToString("short-key-15chr".getBytes());
    CanvasApiTokenSecurityService service = new CanvasApiTokenSecurityService(invalidLengthKey);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> service.encrypt("test-api-token"));
    assertEquals(
        "Canvas API token encryption key must decode to 16, 24, or 32 bytes",
        exception.getMessage());
  }
}
