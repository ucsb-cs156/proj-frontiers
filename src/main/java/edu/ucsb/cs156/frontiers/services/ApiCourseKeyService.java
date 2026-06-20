package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.ApiCourseKey;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.ApiCourseKeyRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class ApiCourseKeyService {
  public enum ExpirationChoice {
    DAYS_90,
    MONTHS_6
  }

  public static final String API_KEY_HEADER = "X-API-KEY";

  public record GeneratedApiCourseKey(String rawKey, ApiCourseKey apiCourseKey) {}

  private final ApiCourseKeyRepository apiCourseKeyRepository;
  private final CourseRepository courseRepository;
  private final SecureRandom secureRandom = new SecureRandom();

  public ApiCourseKeyService(
      ApiCourseKeyRepository apiCourseKeyRepository, CourseRepository courseRepository) {
    this.apiCourseKeyRepository = apiCourseKeyRepository;
    this.courseRepository = courseRepository;
  }

  public GeneratedApiCourseKey createKey(
      Long courseId, User creator, ExpirationChoice expirationChoice) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    String rawKey = generateRawKey();
    String salt = randomHex(16);
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime expiresAt =
        expirationChoice == ExpirationChoice.MONTHS_6 ? now.plusMonths(6) : now.plusDays(90);

    ApiCourseKey apiCourseKey =
        ApiCourseKey.builder()
            .course(course)
            .createdBy(creator)
            .keyHash(hashWithSalt(rawKey, salt))
            .salt(salt)
            .keySuffix(rawKey.substring(Math.max(0, rawKey.length() - 6)))
            .createdAt(now)
            .expiresAt(expiresAt)
            .build();

    ApiCourseKey saved = apiCourseKeyRepository.save(apiCourseKey);
    return new GeneratedApiCourseKey(rawKey, saved);
  }

  public List<ApiCourseKey> listActiveKeys(Long courseId) {
    return apiCourseKeyRepository
        .findByCourseIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            courseId, ZonedDateTime.now());
  }

  public ApiCourseKey revokeKey(Long courseId, Long apiKeyId) {
    ApiCourseKey key =
        apiCourseKeyRepository
            .findByIdAndCourseId(apiKeyId, courseId)
            .orElseThrow(() -> new EntityNotFoundException(ApiCourseKey.class, apiKeyId));
    key.setRevoked(true);
    return apiCourseKeyRepository.save(key);
  }

  public boolean authenticateFromRequestForCourse(Long courseId) {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
      return false;
    }
    HttpServletRequest request = servletRequestAttributes.getRequest();
    String rawKey = request.getHeader(API_KEY_HEADER);
    return authenticateRawKeyForCourse(rawKey, courseId).isPresent();
  }

  public Optional<ApiCourseKey> authenticateRawKeyForCourse(String rawKey, Long courseId) {
    if (rawKey == null || rawKey.isBlank()) {
      return Optional.empty();
    }

    List<ApiCourseKey> activeKeys = listActiveKeys(courseId);
    for (ApiCourseKey key : activeKeys) {
      if (hashMatches(rawKey, key)) {
        key.setUsageCount(key.getUsageCount() + 1);
        key.setLastUsedAt(ZonedDateTime.now());
        return Optional.of(apiCourseKeyRepository.save(key));
      }
    }
    return Optional.empty();
  }

  private String generateRawKey() {
    byte[] raw = new byte[32];
    secureRandom.nextBytes(raw);
    return "frt_" + Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
  }

  private String randomHex(int bytes) {
    byte[] raw = new byte[bytes];
    secureRandom.nextBytes(raw);
    StringBuilder sb = new StringBuilder();
    for (byte b : raw) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private String hashWithSalt(String rawKey, String salt) {
    return Base64.getEncoder().encodeToString(hashBytes(rawKey, salt));
  }

  private byte[] hashBytes(String rawKey, String salt) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest((salt + rawKey).getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  private boolean hashMatches(String rawKey, ApiCourseKey key) {
    byte[] candidate = hashBytes(rawKey, key.getSalt());
    try {
      byte[] expected = Base64.getDecoder().decode(key.getKeyHash());
      return MessageDigest.isEqual(candidate, expected);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
