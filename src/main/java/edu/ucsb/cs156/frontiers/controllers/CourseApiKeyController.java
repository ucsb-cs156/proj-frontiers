package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseApiKey;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.models.CourseApiKeyView;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.ApiKeyService;
import edu.ucsb.cs156.frontiers.services.ApiKeyService.ExpirationChoice;
import edu.ucsb.cs156.frontiers.services.ApiKeyService.IssuedCourseApiKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Course API Key")
@RestController
@Slf4j
@RequestMapping("/api/courses/key")
@Validated
public class CourseApiKeyController extends ApiController {

  private final ApiKeyService apiKeyService;
  private final CourseRepository courseRepository;

  public CourseApiKeyController(ApiKeyService apiKeyService, CourseRepository courseRepository) {
    super();
    this.apiKeyService = apiKeyService;
    this.courseRepository = courseRepository;
  }

  @Operation(
      summary = "Create an API key for a course (instructor only)",
      description =
          "The key is returned once, in this response, and cannot be retrieved again. "
              + "Requires the ENABLE_API_KEYS course option.")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @PostMapping("")
  public IssuedCourseApiKey createApiKey(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "choice") @RequestParam ExpirationChoice choice,
      @Parameter(name = "label", description = "Optional short name for the key (at most 60 chars)")
          @RequestParam(required = false)
          @Size(max = 60, message = "label must be at most 60 characters")
          String label) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    if (!apiKeyService.apiKeysEnabled(courseId)) {
      throw new IllegalArgumentException(
          "The course option ENABLE_API_KEYS must be enabled to create an API key.");
    }
    return apiKeyService.createApiKey(course, choice, label);
  }

  @Operation(
      summary = "List the API keys of a course (instructor only)",
      description =
          "Every key ever created for the course, newest first, with its status "
              + "(ACTIVE, EXPIRED or REVOKED) and usage. The keys themselves are never returned.")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @GetMapping("")
  public List<CourseApiKeyView> listApiKeys(
      @Parameter(name = "courseId") @RequestParam Long courseId) {
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    return apiKeyService.listApiKeys(courseId);
  }

  @Operation(summary = "Revoke one API key of a course, by id (instructor only)")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @DeleteMapping("")
  public Object revokeApiKeyById(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "id") @RequestParam Long id) {
    if (!apiKeyService.revokeApiKeyById(id, courseId)) {
      throw new EntityNotFoundException(CourseApiKey.class, id);
    }
    return genericMessage("API key with id %d revoked".formatted(id));
  }

  @Operation(
      summary = "Revoke an API key, given the key itself (no login needed)",
      description = "For scripts that hold a key and need to disable it, e.g. after a leak.")
  @DeleteMapping("/revoke")
  public ResponseEntity<Void> revokeApiKey(
      @Parameter(name = "apiKey") @RequestParam String apiKey) {
    return apiKeyService.revokeApiKey(apiKey)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }
}
