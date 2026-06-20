package edu.ucsb.cs156.frontiers.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.DownloadRequestRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.ApiCourseKeyService;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;

public class CourseSecurityUnitTests {
  private CourseRepository courseRepository;
  private ApiCourseKeyService apiCourseKeyService;
  private CourseSecurity courseSecurity;

  @BeforeEach
  public void setup() {
    CurrentUserService currentUserService = mock(CurrentUserService.class);
    RoleHierarchy roleHierarchy = mock(RoleHierarchy.class);
    courseRepository = mock(CourseRepository.class);
    RosterStudentRepository rosterStudentRepository = mock(RosterStudentRepository.class);
    DownloadRequestRepository downloadRequestRepository = mock(DownloadRequestRepository.class);
    apiCourseKeyService = mock(ApiCourseKeyService.class);
    courseSecurity =
        new CourseSecurity(
            currentUserService,
            roleHierarchy,
            courseRepository,
            rosterStudentRepository,
            downloadRequestRepository,
            apiCourseKeyService);
  }

  @Test
  public void hasManagePermissionsOrApiKeyAccess_returns_true_when_api_key_valid() {
    when(apiCourseKeyService.authenticateFromRequestForCourse(1L)).thenReturn(true);

    Boolean result =
        courseSecurity.hasManagePermissionsOrApiKeyAccess(
            mock(MethodSecurityExpressionOperations.class), 1L);

    assertTrue(result);
  }

  @Test
  public void hasManagePermissionsOrApiKeyAccess_falls_back_to_manage_permissions() {
    when(apiCourseKeyService.authenticateFromRequestForCourse(1L)).thenReturn(false);
    when(courseRepository.findById(1L)).thenReturn(Optional.empty());

    Boolean result =
        courseSecurity.hasManagePermissionsOrApiKeyAccess(
            mock(MethodSecurityExpressionOperations.class), 1L);

    assertTrue(result);
  }
}
