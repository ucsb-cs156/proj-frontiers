package edu.ucsb.cs156.frontiers.services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

@Service
public class TokenEncryptionService {

  private final AesBytesEncryptor encryptor;

  public TokenEncryptionService(@Value("${app.secret}") String secret) {
    byte[] keyBytes = Hex.decode(secret);
    SecretKey key = new SecretKeySpec(keyBytes, "AES");
    this.encryptor =
        new AesBytesEncryptor(
            key, KeyGenerators.secureRandom(12), AesBytesEncryptor.CipherAlgorithm.GCM);
  }

  public String encryptToken(String token) {
    byte[] encrypted = encryptor.encrypt(token.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(encrypted);
  }

  public String decryptToken(String encryptedToken) {
    byte[] decrypted = encryptor.decrypt(Base64.getDecoder().decode(encryptedToken));
    return new String(decrypted, StandardCharsets.UTF_8);
  }
}
