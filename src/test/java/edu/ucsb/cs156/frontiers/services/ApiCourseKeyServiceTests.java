package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.ApiCourseKey;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.repositories.ApiCourseKeyRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
