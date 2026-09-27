package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.config.ApiKeyToken;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseApiKey;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.repositories.CourseApiKeyRepository;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.Hibernate;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {
  private final DateTimeProvider dateTimeProvider;
  private final CourseApiKeyRepository courseApiKeyRepository;
  private final CurrentUserService currentUserService;
  private final SecureRandom secureRandom;

  public ApiKeyService(
      DateTimeProvider dateTimeProvider,
      CourseApiKeyRepository courseApiKeyRepository,
      CurrentUserService currentUserService,
      SecureRandom secureRandom) {
    this.dateTimeProvider = dateTimeProvider;
    this.courseApiKeyRepository = courseApiKeyRepository;
    this.currentUserService = currentUserService;
    this.secureRandom = secureRandom;
  }

  public record IssuedCourseApiKey(
      String key, Long courseId, ZonedDateTime issuedAt, ZonedDateTime expiresAt) {}

  public enum ExpirationChoice {
    DAYS_90,
    MONTHS_6
  }

  @Transactional
  public IssuedCourseApiKey createApiKey(Course course, ExpirationChoice choice) {
    byte[] nextBytes = new byte[16];
    secureRandom.nextBytes(nextBytes);
    String key = Base64.getUrlEncoder().withoutPadding().encodeToString(nextBytes);

    ZonedDateTime currentTime = ZonedDateTime.from(dateTimeProvider.getNow().get());

    CourseApiKey keyEntity =
        CourseApiKey.builder()
            .course(course)
            .createdAt(currentTime)
            .expiresAt(
                choice == ExpirationChoice.DAYS_90
                    ? currentTime.plusDays(90)
                    : currentTime.plusMonths(6))
            .keyHash(DigestUtils.sha256Hex(key))
            .keySuffix(key.substring(key.length() - 6))
            .createdBy(currentUserService.getUser())
            .build();

    courseApiKeyRepository.save(keyEntity);

    return new IssuedCourseApiKey(key, course.getId(), currentTime, keyEntity.getExpiresAt());
  }

  @Transactional
  public ApiKeyToken authenticateKey(String key) {
    CourseApiKey foundKey =
        courseApiKeyRepository
            .findByKeyHash(DigestUtils.sha256Hex(key))
            .orElseThrow(() -> new AccessDeniedException("Invalid API key"));
    if (foundKey.getRevoked()) {
      throw new AccessDeniedException("API key has been revoked");
    }
    if (foundKey.getExpiresAt().isBefore(ZonedDateTime.from(dateTimeProvider.getNow().get()))) {
      throw new AccessDeniedException("API key has expired");
    }
    /* Forcibly load key owner for auth purposes */
    User owner = Hibernate.unproxy(foundKey.getCreatedBy(), User.class);
    foundKey.setCreatedBy(owner);
    foundKey.setUsageCount(foundKey.getUsageCount() + 1);
    foundKey.setLastUsedAt(ZonedDateTime.from(dateTimeProvider.getNow().get()));
    courseApiKeyRepository.save(foundKey);
    HashSet<GrantedAuthority> authority = new HashSet<>();
    authority.add(new SimpleGrantedAuthority("ROLE_API_KEY"));
    return new ApiKeyToken(foundKey, owner, authority);
  }

  public List<CourseApiKey> getApiKeysForCourse(Long courseId) {
    return courseApiKeyRepository.findByCourseId(courseId);
  }

  public List<CourseApiKey> getApiKeysForUser(User user) {
    return courseApiKeyRepository.findByCreatedBy(user);
  }

  public boolean revokeApiKey(String key) {
    CourseApiKey foundKey =
        courseApiKeyRepository.findByKeyHash(DigestUtils.sha256Hex(key)).orElse(null);
    if (foundKey != null) {
      foundKey.setRevoked(true);
      courseApiKeyRepository.save(foundKey);
      return true;
    } else {
      return false;
    }
  }
}
