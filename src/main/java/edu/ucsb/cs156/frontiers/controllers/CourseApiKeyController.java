package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.ApiKeyService;
import edu.ucsb.cs156.frontiers.services.ApiKeyService.ExpirationChoice;
import edu.ucsb.cs156.frontiers.services.ApiKeyService.IssuedCourseApiKey;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Course API Key")
@RestController
@Slf4j
@RequestMapping("/api/courses/key")
public class CourseApiKeyController extends ApiController {

  private final ApiKeyService apiKeyService;
  private final CourseRepository courseRepository;

  public CourseApiKeyController(ApiKeyService apiKeyService, CourseRepository courseRepository) {
    super();
    this.apiKeyService = apiKeyService;
    this.courseRepository = courseRepository;
  }

  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @PostMapping("")
  public IssuedCourseApiKey createApiKey(
      @RequestParam Long courseId, @RequestParam ExpirationChoice choice) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
    return apiKeyService.createApiKey(course, choice);
  }

  @DeleteMapping("/revoke")
  public ResponseEntity<Void> revokeApiKey(@RequestParam String apiKey) {
    return apiKeyService.revokeApiKey(apiKey)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }
}
