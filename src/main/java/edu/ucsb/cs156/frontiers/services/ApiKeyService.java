package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.config.ApiKeyToken;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseApiKey;
import edu.ucsb.cs156.frontiers.entities.CourseOption;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.CourseOptions;
import edu.ucsb.cs156.frontiers.models.CourseApiKeyView;
import edu.ucsb.cs156.frontiers.repositories.CourseApiKeyRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseOptionRepository;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
  private final CourseOptionRepository courseOptionRepository;

  public ApiKeyService(
      DateTimeProvider dateTimeProvider,
      CourseApiKeyRepository courseApiKeyRepository,
      CurrentUserService currentUserService,
      SecureRandom secureRandom,
      CourseOptionRepository courseOptionRepository) {
    this.dateTimeProvider = dateTimeProvider;
    this.courseApiKeyRepository = courseApiKeyRepository;
    this.currentUserService = currentUserService;
    this.secureRandom = secureRandom;
    this.courseOptionRepository = courseOptionRepository;
  }

  /**
   * Whether the ENABLE_API_KEYS course option is on for the course. Keys can only be created for,
   * and used with, a course that has the option on; turning it off disables every key of the course
   * at once.
   *
   * @param courseId the id of the course
   * @return true if the option is enabled, false if it is disabled or has never been set
   */
  public boolean apiKeysEnabled(Long courseId) {
    return courseOptionRepository
        .findByCourseIdAndOption(courseId, CourseOptions.ENABLE_API_KEYS.name())
        .map(CourseOption::getEnabled)
        .orElse(false);
  }

  /**
   * A freshly created key. This is the only time the key itself is available; only its hash is
   * stored.
   */
  public record IssuedCourseApiKey(
      String key, Long courseId, ZonedDateTime issuedAt, ZonedDateTime expiresAt, String label) {}

  public enum ExpirationChoice {
    DAYS_90,
    MONTHS_6
  }

  /**
   * Creates a key for the course.
   *
   * @param course the course the key is for
   * @param choice how long the key lasts
   * @param label an optional short name for the key; blank is stored as null
   * @return the key and its metadata
   */
  @Transactional
  public IssuedCourseApiKey createApiKey(Course course, ExpirationChoice choice, String label) {
    String trimmedLabel = label == null || label.isBlank() ? null : label.strip();
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
            .label(trimmedLabel)
            .build();

    courseApiKeyRepository.save(keyEntity);

    return new IssuedCourseApiKey(
        key, course.getId(), currentTime, keyEntity.getExpiresAt(), trimmedLabel);
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
    if (!apiKeysEnabled(foundKey.getCourse().getId())) {
      throw new AccessDeniedException("API keys are not enabled for this course");
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

  /**
   * The keys of a course as an instructor may see them (never the key itself), newest first.
   *
   * @param courseId the id of the course
   * @return one view per key, including revoked and expired ones
   */
  @Transactional
  public List<CourseApiKeyView> listApiKeys(Long courseId) {
    ZonedDateTime now = ZonedDateTime.from(dateTimeProvider.getNow().get());
    return courseApiKeyRepository.findByCourseId(courseId).stream()
        .sorted(Comparator.comparing(CourseApiKey::getCreatedAt).reversed())
        .map(key -> CourseApiKeyView.from(key, now))
        .toList();
  }

  /**
   * Revokes a key by id, for the instructor's key table. The course id must match the key's, so
   * that an instructor of one course cannot revoke another course's keys by guessing ids.
   *
   * @param id the key's id
   * @param courseId the course the key is expected to belong to
   * @return true if the key was found in that course (and is now revoked), false otherwise
   */
  @Transactional
  public boolean revokeApiKeyById(Long id, Long courseId) {
    Optional<CourseApiKey> found = courseApiKeyRepository.findById(id);
    if (found.isEmpty() || !Objects.equals(found.get().getCourse().getId(), courseId)) {
      return false;
    }
    found.get().setRevoked(true);
    courseApiKeyRepository.save(found.get());
    return true;
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
