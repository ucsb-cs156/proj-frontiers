package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TokenEncryptionServiceTests {

  private TokenEncryptionService tokenEncryptionService;

  private static final String TEST_SECRET =
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

  @BeforeEach
  public void setup() {
    tokenEncryptionService = new TokenEncryptionService(TEST_SECRET);
  }

  @Test
  public void testEncryptAndDecrypt_roundtrip() {
    String original = "fake-canvas-token-1234";
    String encrypted = tokenEncryptionService.encryptToken(original);
    String decrypted = tokenEncryptionService.decryptToken(encrypted);
    assertEquals(original, decrypted);
  }

  @Test
  public void testEncrypt_isNonDeterministic() {
    String token = "fake-canvas-token-1234";
    String encrypted1 = tokenEncryptionService.encryptToken(token);
    String encrypted2 = tokenEncryptionService.encryptToken(token);
    assertNotEquals(encrypted1, encrypted2);
  }

  @Test
  public void testDecrypt_throwsOnTamperedCiphertext() {
    String encrypted = tokenEncryptionService.encryptToken("fake-canvas-token-1234");
    String tampered = encrypted.substring(0, encrypted.length() - 4) + "XXXX";
    assertThrows(Exception.class, () -> tokenEncryptionService.decryptToken(tampered));
  }

  @Test
  public void testDecrypt_throwsOnGarbageInput() {
    assertThrows(Exception.class, () -> tokenEncryptionService.decryptToken("notvalidbase64!!!"));
  }

  @Test
  public void testEncryptToken_nullOrEmpty() {
    assertEquals(null, tokenEncryptionService.encryptToken(null));
    assertEquals(null, tokenEncryptionService.encryptToken(""));
  }
}
