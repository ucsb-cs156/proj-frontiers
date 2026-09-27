package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.config.ApiKeyToken;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseApiKey;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.repositories.CourseApiKeyRepository;
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
  private SecureRandom actualSecureRandom = new SecureRandom();

  private final ZonedDateTime staticZD =
      Instant.parse("2026-03-11T08:00:00.00Z").atZone(ZoneId.of("America/Los_Angeles"));

  @InjectMocks private ApiKeyService apiKeyService;

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
        apiKeyService.createApiKey(course, ExpirationChoice.DAYS_90);
    verify(apiKeyRepository).save(argumentCaptor.capture());
    verify(secureRandom).nextBytes(any(byte[].class));
    CourseApiKey savedApiKey = argumentCaptor.getValue();

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
        apiKeyService.createApiKey(course, ExpirationChoice.MONTHS_6);
    verify(apiKeyRepository).save(argumentCaptor.capture());
    verify(secureRandom).nextBytes(any(byte[].class));
    CourseApiKey savedApiKey = argumentCaptor.getValue();

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
