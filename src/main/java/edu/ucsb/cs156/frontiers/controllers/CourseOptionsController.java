package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.CourseOption;
import edu.ucsb.cs156.frontiers.enums.CourseOptions;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseOptionRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CourseOptions")
@RequestMapping("/api/course/options")
@RestController
public class CourseOptionsController extends ApiController {

  @Autowired private CourseOptionRepository courseOptionRepository;

  @Autowired private CourseRepository courseRepository;

  @Operation(summary = "Get toggleable options for a course")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @GetMapping("")
  public Map<String, Boolean> getCourseOptions(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "option") @RequestParam(required = false) String option) {
    ensureCourseExists(courseId);

    if (option != null && !option.isBlank()) {
      String normalizedOption = normalizeAndValidateOption(option);
      boolean enabled =
          courseOptionRepository
              .findByCourseIdAndOption(courseId, normalizedOption)
              .map(CourseOption::getEnabled)
              .orElse(false);
      return Map.of(normalizedOption, enabled);
    }

    LinkedHashMap<String, Boolean> options = new LinkedHashMap<>();
    for (CourseOptions courseOption : CourseOptions.values()) {
      options.put(courseOption.name(), false);
    }
    for (CourseOption courseOption : courseOptionRepository.findByCourseId(courseId)) {
      if (options.containsKey(courseOption.getOption())) {
        options.put(courseOption.getOption(), courseOption.getEnabled());
      }
    }
    return options;
  }

  @Operation(summary = "Set toggleable option for a course")
  @PreAuthorize("@CourseSecurity.hasInstructorPermissions(#root, #courseId)")
  @PostMapping("")
  public Map<String, Boolean> setCourseOption(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "option") @RequestParam String option,
      @Parameter(name = "enabled") @RequestParam Boolean enabled) {
    ensureCourseExists(courseId);

    String normalizedOption = normalizeAndValidateOption(option);
    CourseOption courseOption =
        courseOptionRepository
            .findByCourseIdAndOption(courseId, normalizedOption)
            .orElse(CourseOption.builder().courseId(courseId).option(normalizedOption).build());
    courseOption.setEnabled(enabled);
    courseOptionRepository.save(courseOption);

    return Map.of(normalizedOption, enabled);
  }

  private void ensureCourseExists(Long courseId) {
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
  }

  private String normalizeAndValidateOption(String option) {
    String normalizedOption = option.strip().toUpperCase();
    try {
      CourseOptions.valueOf(normalizedOption);
      return normalizedOption;
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid course option: " + option);
    }
  }
}
