package edu.ucsb.cs156.frontiers.services;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CanvasApiTokenSecurityService {

  private static final String ENCRYPTION_PREFIX = "enc:v1:";
  private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final int GCM_IV_LENGTH_BYTES = 12;

  private final String encodedEncryptionKey;
  private final SecureRandom secureRandom = new SecureRandom();

  public CanvasApiTokenSecurityService(
      @Value("${app.canvas.api-token-encryption-key:}") String encodedEncryptionKey) {
    this.encodedEncryptionKey = encodedEncryptionKey;
  }

  public String encrypt(String canvasApiToken) {
    if (canvasApiToken == null || canvasApiToken.isEmpty()) {
      return canvasApiToken;
    }
    if (canvasApiToken.startsWith(ENCRYPTION_PREFIX)) {
      return canvasApiToken;
    }

    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);
      cipher.init(
          Cipher.ENCRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

      byte[] ciphertext = cipher.doFinal(canvasApiToken.getBytes(StandardCharsets.UTF_8));
      byte[] payload = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, payload, 0, iv.length);
      System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
      return ENCRYPTION_PREFIX + Base64.getEncoder().encodeToString(payload);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Failed to encrypt Canvas API token", e);
    }
  }

  public String decrypt(String storedCanvasApiToken) {
    if (storedCanvasApiToken == null || storedCanvasApiToken.isEmpty()) {
      return storedCanvasApiToken;
    }
    if (!storedCanvasApiToken.startsWith(ENCRYPTION_PREFIX)) {
      return storedCanvasApiToken;
    }

    try {
      byte[] payload =
          Base64.getDecoder().decode(storedCanvasApiToken.substring(ENCRYPTION_PREFIX.length()));
      if (payload.length <= GCM_IV_LENGTH_BYTES) {
        throw new IllegalStateException("Encrypted Canvas API token is malformed");
      }
      byte[] iv = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH_BYTES);
      byte[] ciphertext = Arrays.copyOfRange(payload, GCM_IV_LENGTH_BYTES, payload.length);

      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(
          Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
      byte[] plaintext = cipher.doFinal(ciphertext);
      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new IllegalStateException("Failed to decrypt Canvas API token", e);
    }
  }

  private SecretKeySpec getSecretKey() {
    if (encodedEncryptionKey == null || encodedEncryptionKey.isBlank()) {
      throw new IllegalStateException("Canvas API token encryption key is not configured");
    }
    byte[] decodedKey;
    try {
      decodedKey = Base64.getDecoder().decode(encodedEncryptionKey);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Canvas API token encryption key must be Base64 encoded", e);
    }
    if (decodedKey.length != 16 && decodedKey.length != 24 && decodedKey.length != 32) {
      throw new IllegalStateException(
          "Canvas API token encryption key must decode to 16, 24, or 32 bytes");
    }
    return new SecretKeySpec(decodedKey, "AES");
  }
}
