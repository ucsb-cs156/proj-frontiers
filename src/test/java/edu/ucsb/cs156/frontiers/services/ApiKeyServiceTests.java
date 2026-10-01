package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.config.ApiKeyToken;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseApiKey;
import edu.ucsb.cs156.frontiers.entities.CourseOption;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.models.CourseApiKeyView;
import edu.ucsb.cs156.frontiers.models.CourseApiKeyView.Status;
import edu.ucsb.cs156.frontiers.repositories.CourseApiKeyRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseOptionRepository;
import edu.ucsb.cs156.frontiers.services.ApiKeyService.ExpirationChoice;
import edu.ucsb.cs156.frontiers.services.ApiKeyService.IssuedCourseApiKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
public class ApiKeyServiceTests {

  @Mock private DateTimeProvider provider;
  @Mock private CourseApiKeyRepository apiKeyRepository;
  @Mock private CurrentUserService currentUserService;
  @Mock private SecureRandom secureRandom;
  @Mock private CourseOptionRepository courseOptionRepository;
  private SecureRandom actualSecureRandom = new SecureRandom();

  private final ZonedDateTime staticZD =
      Instant.parse("2026-03-11T08:00:00.00Z").atZone(ZoneId.of("America/Los_Angeles"));

  @InjectMocks private ApiKeyService apiKeyService;

  private void apiKeysOption(Long courseId, Boolean enabled) {
    when(courseOptionRepository.findByCourseIdAndOption(courseId, "ENABLE_API_KEYS"))
        .thenReturn(
            enabled == null
                ? Optional.empty()
                : Optional.of(
                    CourseOption.builder()
                        .courseId(courseId)
                        .option("ENABLE_API_KEYS")
                        .enabled(enabled)
                        .build()));
  }

  @Test
  public void apiKeysEnabled_reflects_the_course_option() {
    apiKeysOption(1L, true);
    apiKeysOption(2L, false);
    apiKeysOption(3L, null);
    assertTrue(apiKeyService.apiKeysEnabled(1L));
    assertFalse(apiKeyService.apiKeysEnabled(2L));
    assertFalse(apiKeyService.apiKeysEnabled(3L));
  }

  @Test
  public void authenticate_rejects_key_when_api_keys_are_disabled_for_the_course() {
    when(provider.getNow()).thenReturn(Optional.of(staticZD));
    Course course = Course.builder().id(1L).build();
    CourseApiKey apiKey =
        CourseApiKey.builder().course(course).expiresAt(staticZD.plusDays(90)).build();
    when(apiKeyRepository.findByKeyHash(DigestUtils.sha256Hex("valid-key")))
        .thenReturn(Optional.of(apiKey));
    apiKeysOption(1L, false);

    AccessDeniedException thrown =
        assertThrows(AccessDeniedException.class, () -> apiKeyService.authenticateKey("valid-key"));
    assertEquals("API keys are not enabled for this course", thrown.getMessage());
    verify(apiKeyRepository, never()).save(any());
  }

  @Test
  public void authenticate_rejects_key_when_api_keys_option_was_never_set() {
    when(provider.getNow()).thenReturn(Optional.of(staticZD));
    Course course = Course.builder().id(1L).build();
    CourseApiKey apiKey =
        CourseApiKey.builder().course(course).expiresAt(staticZD.plusDays(90)).build();
    when(apiKeyRepository.findByKeyHash(DigestUtils.sha256Hex("valid-key")))
        .thenReturn(Optional.of(apiKey));
    apiKeysOption(1L, null);

    assertThrows(AccessDeniedException.class, () -> apiKeyService.authenticateKey("valid-key"));
    verify(apiKeyRepository, never()).save(any());
  }

  @Test
  public void key_generates_correctly() {
    when(provider.getNow()).thenReturn(Optional.of(staticZD));
    Course course = Course.builder().id(2L).build();
    User user = User.builder().id(1L).build();
    when(currentUserService.getUser()).thenReturn(user);
    doAnswer(
            invocation -> {
              byte[] bytes = invocation.getArgument(0);
              actualSecureRandom.nextBytes(bytes);
              return null;
            })
        .when(secureRandom)
        .nextBytes(any(byte[].class));
    ArgumentCaptor<CourseApiKey> argumentCaptor = ArgumentCaptor.forClass(CourseApiKey.class);

    IssuedCourseApiKey issuedCourseApiKey =
        apiKeyService.createApiKey(course, ExpirationChoice.DAYS_90, "  jpa02 autograder ");
    verify(apiKeyRepository).save(argumentCaptor.capture());
    verify(secureRandom).nextBytes(any(byte[].class));
    CourseApiKey savedApiKey = argumentCaptor.getValue();

    assertEquals("jpa02 autograder", savedApiKey.getLabel());
    assertEquals("jpa02 autograder", issuedCourseApiKey.label());
    assertEquals(issuedCourseApiKey.courseId(), course.getId());
    assertEquals(issuedCourseApiKey.issuedAt(), staticZD);
    assertEquals(staticZD.plusDays(90), issuedCourseApiKey.expiresAt());
    assertEquals(course.getId(), savedApiKey.getCourse().getId());
    assertEquals(user.getId(), savedApiKey.getCreatedBy().getId());
    assertEquals(staticZD, issuedCourseApiKey.issuedAt());
    assertEquals(staticZD.plusDays(90), issuedCourseApiKey.expiresAt());
    assertEquals(DigestUtils.sha256Hex(issuedCourseApiKey.key()), savedApiKey.getKeyHash());
    assertEquals(
        issuedCourseApiKey.key().substring(issuedCourseApiKey.key().length() - 6),
        savedApiKey.getKeySuffix());
  }

  @Test
  public void key_generates_correctly_6mo() {
    when(provider.getNow()).thenReturn(Optional.of(staticZD));
    Course course = Course.builder().id(2L).build();
    User user = User.builder().id(1L).build();
    when(currentUserService.getUser()).thenReturn(user);
    doAnswer(
            invocation -> {
              byte[] bytes = invocation.getArgument(0);
              actualSecureRandom.nextBytes(bytes);
              return null;
            })
        .when(secureRandom)
        .nextBytes(any(byte[].class));
    ArgumentCaptor<CourseApiKey> argumentCaptor = ArgumentCaptor.forClass(CourseApiKey.class);

    IssuedCourseApiKey issuedCourseApiKey =
        apiKeyService.createApiKey(course, ExpirationChoice.MONTHS_6, null);
    verify(apiKeyRepository).save(argumentCaptor.capture());
    verify(secureRandom).nextBytes(any(byte[].class));
    CourseApiKey savedApiKey = argumentCaptor.getValue();

    assertEquals(null, savedApiKey.getLabel());
    assertEquals(null, issuedCourseApiKey.label());

    assertEquals(issuedCourseApiKey.courseId(), course.getId());
    assertEquals(issuedCourseApiKey.issuedAt(), staticZD);
    assertEquals(staticZD.plusMonths(6), issuedCourseApiKey.expiresAt());
    assertEquals(course.getId(), savedApiKey.getCourse().getId());
    assertEquals(user.getId(), savedApiKey.getCreatedBy().getId());
    assertEquals(staticZD, issuedCourseApiKey.issuedAt());
    assertEquals(staticZD.plusMonths(6), issuedCourseApiKey.expiresAt());
    assertEquals(DigestUtils.sha256Hex(issuedCourseApiKey.key()), savedApiKey.getKeyHash());
    assertEquals(
        issuedCourseApiKey.key().substring(issuedCourseApiKey.key().length() - 6),
        savedApiKey.getKeySuffix());
  }

  @Test
  public void blank_label_is_stored_as_null() {
    when(provider.getNow()).thenReturn(Optional.of(staticZD));
    Course course = Course.builder().id(2L).build();
    when(currentUserService.getUser()).thenReturn(User.builder().id(1L).build());
    ArgumentCaptor<CourseApiKey> argumentCaptor = ArgumentCaptor.forClass(CourseApiKey.class);

    IssuedCourseApiKey issued = apiKeyService.createApiKey(course, ExpirationChoice.DAYS_90, "   ");
    verify(apiKeyRepository).save(argumentCaptor.capture());

    assertEquals(null, argumentCaptor.getValue().getLabel());
    assertEquals(null, issued.label());
  }

  @Test
  public void listApiKeys_returns_views_newest_first_with_status() {
    when(provider.getNow()).thenReturn(Optional.of(staticZD));
    Course course = Course.builder().id(1L).build();
    User creator = User.builder().id(1L).email("phtcon@ucsb.edu").build();

    CourseApiKey active =
        CourseApiKey.builder()
            .id(1L)
            .course(course)
            .createdBy(creator)
            .label("jpa02 autograder")
            .keySuffix("abc123")
            .createdAt(staticZD.minusDays(2))
            .expiresAt(staticZD.plusDays(88))
            .lastUsedAt(staticZD.minusHours(1))
            .usageCount(3L)
            .build();
    CourseApiKey expired =
        CourseApiKey.builder()
            .id(2L)
            .course(course)
            .createdBy(creator)
            .keySuffix("def456")
            .createdAt(staticZD.minusDays(1))
            .expiresAt(staticZD.minusMinutes(1))
            .build();
    CourseApiKey revokedAndExpired =
        CourseApiKey.builder()
            .id(3L)
            .course(course)
            .createdBy(creator)
            .keySuffix("ghi789")
            .createdAt(staticZD.minusDays(3))
            .expiresAt(staticZD.minusDays(1))
            .revoked(true)
            .build();
    CourseApiKey expiresRightNow =
        CourseApiKey.builder()
            .id(4L)
            .course(course)
            .createdBy(creator)
            .keySuffix("jkl012")
            .createdAt(staticZD.minusDays(4))
            .expiresAt(staticZD)
            .build();
    when(apiKeyRepository.findByCourseId(1L))
        .thenReturn(List.of(active, revokedAndExpired, expiresRightNow, expired));

    List<CourseApiKeyView> expected =
        List.of(
            new CourseApiKeyView(
                2L,
                null,
                "def456",
                "phtcon@ucsb.edu",
                staticZD.minusDays(1),
                staticZD.minusMinutes(1),
                null,
                0L,
                false,
                Status.EXPIRED),
            new CourseApiKeyView(
                1L,
                "jpa02 autograder",
                "abc123",
                "phtcon@ucsb.edu",
                staticZD.minusDays(2),
                staticZD.plusDays(88),
                staticZD.minusHours(1),
                3L,
                false,
                Status.ACTIVE),
            new CourseApiKeyView(
                3L,
                null,
                "ghi789",
                "phtcon@ucsb.edu",
                staticZD.minusDays(3),
                staticZD.minusDays(1),
                null,
                0L,
                true,
                Status.REVOKED),
            new CourseApiKeyView(
                4L,
                null,
                "jkl012",
                "phtcon@ucsb.edu",
                staticZD.minusDays(4),
                staticZD,
                null,
                0L,
                false,
                Status.ACTIVE));

    assertEquals(expected, apiKeyService.listApiKeys(1L));
  }

  @Test
  public void revokeApiKeyById_revokes_a_key_of_the_course() {
    Course course = Course.builder().id(1L).build();
    CourseApiKey key = CourseApiKey.builder().id(5L).course(course).build();
    when(apiKeyRepository.findById(5L)).thenReturn(Optional.of(key));

    assertTrue(apiKeyService.revokeApiKeyById(5L, 1L));
    assertTrue(key.getRevoked());
    verify(apiKeyRepository).save(key);
  }

  @Test
  public void revokeApiKeyById_refuses_a_key_of_another_course() {
    Course course = Course.builder().id(1L).build();
    CourseApiKey key = CourseApiKey.builder().id(5L).course(course).build();
    when(apiKeyRepository.findById(5L)).thenReturn(Optional.of(key));

    assertFalse(apiKeyService.revokeApiKeyById(5L, 2L));
    assertFalse(key.getRevoked());
    verify(apiKeyRepository, never()).save(any());
  }

  @Test
  public void revokeApiKeyById_returns_false_for_unknown_id() {
    when(apiKeyRepository.findById(5L)).thenReturn(Optional.empty());

    assertFalse(apiKeyService.revokeApiKeyById(5L, 1L));
    verify(apiKeyRepository, never()).save(any());
  }

  @Test
  public void assert_blocks_invalid_key_types() {
    when(provider.getNow()).thenReturn(Optional.of(staticZD));
    when(apiKeyRepository.findByKeyHash(DigestUtils.sha256Hex("invalid-key")))
        .thenReturn(Optional.empty());
    assertThrows(AccessDeniedException.class, () -> apiKeyService.authenticateKey("invalid-key"));

    CourseApiKey revokedKey = CourseApiKey.builder().revoked(true).build();
    when(apiKeyRepository.findByKeyHash(DigestUtils.sha256Hex("invalid-key")))
        .thenReturn(Optional.of(revokedKey));
    assertThrows(AccessDeniedException.class, () -> apiKeyService.authenticateKey("invalid-key"));

    CourseApiKey expiredKey = CourseApiKey.builder().expiresAt(staticZD.minusDays(1)).build();
    when(apiKeyRepository.findByKeyHash(DigestUtils.sha256Hex("invalid-key")))
        .thenReturn(Optional.of(expiredKey));
    assertThrows(AccessDeniedException.class, () -> apiKeyService.authenticateKey("invalid-key"));
  }

  @Test
  public void happy_path_validate_api_key() {
    when(provider.getNow()).thenReturn(Optional.of(staticZD));
    // stand-in for a Hibernate proxy
    User userProxy = User.builder().id(-1L).build();
    User realUser = User.builder().id(1L).build();

    Course course = Course.builder().id(1L).build();

    String keyHash = DigestUtils.sha256Hex("valid-key");
    CourseApiKey apiKey =
        CourseApiKey.builder()
            .course(course)
            .createdBy(userProxy)
            .createdAt(staticZD)
            .expiresAt(staticZD.plusDays(90))
            .build();

    when(apiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(apiKey));
    apiKeysOption(1L, true);
    try (MockedStatic<Hibernate> hibernateMockedStatic = Mockito.mockStatic(Hibernate.class)) {
      hibernateMockedStatic
          .when(() -> Hibernate.unproxy(eq(userProxy), eq(User.class)))
          .thenReturn(realUser);
      ArgumentCaptor<CourseApiKey> apiKeyCaptor = ArgumentCaptor.forClass(CourseApiKey.class);
      ApiKeyToken token = apiKeyService.authenticateKey("valid-key");
      verify(apiKeyRepository).save(apiKeyCaptor.capture());
      CourseApiKey capturedToken = apiKeyCaptor.getValue();
      assertEquals(1L, capturedToken.getUsageCount());
      assertEquals(realUser, capturedToken.getCreatedBy());
      assertEquals(staticZD, capturedToken.getLastUsedAt());

      assertEquals(realUser, token.getPrincipal());
      assertEquals(course.getId(), token.getCourseId());
      assertTrue(token.isAuthenticated());
      assertTrue(token.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_API_KEY")));
    }
  }

  @Test
  public void repoProxies() {
    CourseApiKey returnedKey = CourseApiKey.builder().build();
    when(apiKeyRepository.findByCreatedBy(any(User.class))).thenReturn(List.of(returnedKey));
    when(apiKeyRepository.findByCourseId(any(Long.class))).thenReturn(List.of(returnedKey));

    User test = User.builder().id(1L).build();
    assertEquals(List.of(returnedKey), apiKeyService.getApiKeysForCourse(2L));
    assertEquals(List.of(returnedKey), apiKeyService.getApiKeysForUser(test));

    verify(apiKeyRepository, times(1)).findByCreatedBy(eq(test));
    verify(apiKeyRepository, times(1)).findByCourseId(eq(2L));
  }

  @Test
  public void revoke_correctly_revokes() {
    CourseApiKey test = CourseApiKey.builder().build();
    when(apiKeyRepository.findByKeyHash(DigestUtils.sha256Hex("valid-key")))
        .thenReturn(Optional.of(test));
    assertTrue(apiKeyService.revokeApiKey("valid-key"));
    verify(apiKeyRepository, times(1)).findByKeyHash(eq(DigestUtils.sha256Hex("valid-key")));
    verify(apiKeyRepository, times(1)).save(eq(test));
    assertTrue(test.getRevoked());
  }

  @Test
  public void nonexistent_key_returns_false() {
    when(apiKeyRepository.findByKeyHash(DigestUtils.sha256Hex("invalid-key")))
        .thenReturn(Optional.empty());
    assertFalse(apiKeyService.revokeApiKey("invalid-key"));
    verify(apiKeyRepository, times(1)).findByKeyHash(eq(DigestUtils.sha256Hex("invalid-key")));
    verifyNoMoreInteractions(apiKeyRepository);
  }
}
