package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.ApiCourseKey;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.ApiCourseKeyRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class ApiCourseKeyServiceTests {
  private ApiCourseKeyRepository apiCourseKeyRepository;
  private CourseRepository courseRepository;

  private ApiCourseKeyService service;

  @BeforeEach
  public void setup() {
    apiCourseKeyRepository = mock(ApiCourseKeyRepository.class);
    courseRepository = mock(CourseRepository.class);
    service = new ApiCourseKeyService(apiCourseKeyRepository, courseRepository);
  }

  @AfterEach
  public void teardown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  public void createKey_generates_expected_fields() {
    Course course = Course.builder().id(1L).build();
    User creator = User.builder().id(2L).email("instructor@ucsb.edu").build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(apiCourseKeyRepository.save(any(ApiCourseKey.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ApiCourseKeyService.GeneratedApiCourseKey generated =
        service.createKey(1L, creator, ApiCourseKeyService.ExpirationChoice.DAYS_90);

    assertNotNull(generated.rawKey());
    assertTrue(generated.rawKey().startsWith("frt_"));
    assertEquals(creator, generated.apiCourseKey().getCreatedBy());
    assertNotEquals(generated.rawKey(), generated.apiCourseKey().getKeyHash());
    assertTrue(
        generated.apiCourseKey().getExpiresAt().isAfter(generated.apiCourseKey().getCreatedAt()));
    assertEquals(6, generated.apiCourseKey().getKeySuffix().length());
  }

  @Test
  public void createKey_uses_six_month_expiration_when_selected() {
    Course course = Course.builder().id(1L).build();
    User creator = User.builder().id(2L).email("instructor@ucsb.edu").build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(apiCourseKeyRepository.save(any(ApiCourseKey.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ApiCourseKeyService.GeneratedApiCourseKey generated =
        service.createKey(1L, creator, ApiCourseKeyService.ExpirationChoice.MONTHS_6);

    ZonedDateTime createdAt = generated.apiCourseKey().getCreatedAt();
    ZonedDateTime expiresAt = generated.apiCourseKey().getExpiresAt();
    assertEquals(createdAt.plusMonths(6).toLocalDate(), expiresAt.toLocalDate());
  }

  @Test
  public void createKey_throws_when_course_not_found() {
    User creator = User.builder().id(2L).email("instructor@ucsb.edu").build();
    when(courseRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(
        EntityNotFoundException.class,
        () -> service.createKey(1L, creator, ApiCourseKeyService.ExpirationChoice.DAYS_90));
  }

  @Test
  public void createKey_throws_when_sha256_unavailable() {
    Course course = Course.builder().id(1L).build();
    User creator = User.builder().id(2L).email("instructor@ucsb.edu").build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

    try (MockedStatic<MessageDigest> messageDigestMock = Mockito.mockStatic(MessageDigest.class)) {
      messageDigestMock
          .when(() -> MessageDigest.getInstance("SHA-256"))
          .thenThrow(new NoSuchAlgorithmException("no sha-256"));

      assertThrows(
          IllegalStateException.class,
          () -> service.createKey(1L, creator, ApiCourseKeyService.ExpirationChoice.DAYS_90));
    }
  }

  @Test
  public void authenticateRawKeyForCourse_updates_usage_for_valid_key() {
    Course course = Course.builder().id(1L).build();
    User creator = User.builder().id(2L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(apiCourseKeyRepository.save(any(ApiCourseKey.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ApiCourseKeyService.GeneratedApiCourseKey generated =
        service.createKey(1L, creator, ApiCourseKeyService.ExpirationChoice.DAYS_90);

    when(apiCourseKeyRepository.findByCourseIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            eq(1L), any()))
        .thenReturn(List.of(generated.apiCourseKey()));

    Optional<ApiCourseKey> result = service.authenticateRawKeyForCourse(generated.rawKey(), 1L);

    assertTrue(result.isPresent());
    assertEquals(1, result.get().getUsageCount());
    assertNotNull(result.get().getLastUsedAt());
  }

  @Test
  public void authenticateRawKeyForCourse_returns_empty_for_invalid_key() {
    ApiCourseKey stored =
        ApiCourseKey.builder()
            .salt("abcd")
            .keyHash("not-a-match")
            .expiresAt(ZonedDateTime.now().plusDays(30))
            .revoked(false)
            .build();
    when(apiCourseKeyRepository.findByCourseIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            eq(1L), any()))
        .thenReturn(List.of(stored));

    Optional<ApiCourseKey> result = service.authenticateRawKeyForCourse("frt_wrong", 1L);

    assertTrue(result.isEmpty());
  }

  @Test
  public void authenticateRawKeyForCourse_returns_empty_for_blank_inputs() {
    assertTrue(service.authenticateRawKeyForCourse(null, 1L).isEmpty());
    assertTrue(service.authenticateRawKeyForCourse("   ", 1L).isEmpty());
  }

  @Test
  public void revokeKey_marks_key_revoked() {
    ApiCourseKey stored = ApiCourseKey.builder().id(10L).revoked(false).build();
    when(apiCourseKeyRepository.findByIdAndCourseId(10L, 1L)).thenReturn(Optional.of(stored));
    when(apiCourseKeyRepository.save(any(ApiCourseKey.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ApiCourseKey result = service.revokeKey(1L, 10L);

    assertTrue(result.getRevoked());
  }

  @Test
  public void revokeKey_throws_when_key_missing() {
    when(apiCourseKeyRepository.findByIdAndCourseId(10L, 1L)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> service.revokeKey(1L, 10L));
  }

  @Test
  public void authenticateFromRequestForCourse_returns_false_without_request_context() {
    RequestContextHolder.resetRequestAttributes();
    assertFalse(service.authenticateFromRequestForCourse(1L));
  }

  @Test
  public void authenticateFromRequestForCourse_authenticates_valid_request_header() {
    Course course = Course.builder().id(1L).build();
    User creator = User.builder().id(2L).build();
    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(apiCourseKeyRepository.save(any(ApiCourseKey.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    ApiCourseKeyService.GeneratedApiCourseKey generated =
        service.createKey(1L, creator, ApiCourseKeyService.ExpirationChoice.DAYS_90);
    when(apiCourseKeyRepository.findByCourseIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            eq(1L), any()))
        .thenReturn(List.of(generated.apiCourseKey()));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(ApiCourseKeyService.API_KEY_HEADER, generated.rawKey());
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    assertTrue(service.authenticateFromRequestForCourse(1L));
  }

  @Test
  public void apiCourseKey_isActive_reflects_revocation_and_expiration() {
    ZonedDateTime now = ZonedDateTime.now();
    ApiCourseKey active =
        ApiCourseKey.builder()
            .revoked(false)
            .expiresAt(now.plusDays(1))
            .keyHash("h")
            .salt("s")
            .build();
    ApiCourseKey revoked =
        ApiCourseKey.builder()
            .revoked(true)
            .expiresAt(now.plusDays(1))
            .keyHash("h")
            .salt("s")
            .build();
    ApiCourseKey expired =
        ApiCourseKey.builder()
            .revoked(false)
            .expiresAt(now.minusSeconds(1))
            .keyHash("h")
            .salt("s")
            .build();

    assertTrue(active.isActive(now));
    assertFalse(revoked.isActive(now));
    assertFalse(expired.isActive(now));
  }
}
