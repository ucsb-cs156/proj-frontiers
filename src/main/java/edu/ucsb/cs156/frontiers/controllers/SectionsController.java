package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Section;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.SectionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * CRUD endpoints for the Sections table, scoped by course. Sections map a raw section identifier
 * (e.g. the value that appears in a roster CSV) to a human readable label; they are used when the
 * TRANSLATE_SECTIONS course option is enabled.
 */
@Tag(name = "Sections")
@RequestMapping("/api/courses/{courseId}/sections")
@RestController
@Slf4j
public class SectionsController extends ApiController {

  @Autowired private SectionRepository sectionRepository;

  @Autowired private CourseRepository courseRepository;

  /**
   * List all sections for a course, ordered by section.
   *
   * @param courseId the id of the course
   * @return the sections for the course
   */
  @Operation(summary = "List all sections for a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("")
  public List<Section> getSections(@Parameter(name = "courseId") @PathVariable Long courseId) {
    ensureCourseExists(courseId);
    return sectionRepository.findByCourseIdOrderBySectionAsc(courseId);
  }

  /**
   * Create a new section for a course.
   *
   * @param courseId the id of the course
   * @param section the section identifier (must be unique within the course)
   * @param label the human readable label for the section
   * @return the created section
   */
  @Operation(summary = "Create a new section for a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("")
  public Section postSection(
      @Parameter(name = "courseId") @PathVariable Long courseId,
      @Parameter(name = "section") @RequestParam String section,
      @Parameter(name = "label") @RequestParam String label) {
    Course course = ensureCourseExists(courseId);
    String normalizedSection = normalizeRequired("section", section);
    String normalizedLabel = normalizeRequired("label", label);

    if (sectionRepository.findByCourseIdAndSection(courseId, normalizedSection).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Section %s already exists for course %d".formatted(normalizedSection, courseId));
    }

    Section newSection =
        Section.builder().course(course).section(normalizedSection).label(normalizedLabel).build();
    return sectionRepository.save(newSection);
  }

  /**
   * Update an existing section for a course.
   *
   * @param courseId the id of the course
   * @param id the id of the section to update
   * @param section the new section identifier (must be unique within the course)
   * @param label the new human readable label for the section
   * @return the updated section
   */
  @Operation(summary = "Update a section for a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PutMapping("/{id}")
  public Section putSection(
      @Parameter(name = "courseId") @PathVariable Long courseId,
      @Parameter(name = "id") @PathVariable Long id,
      @Parameter(name = "section") @RequestParam String section,
      @Parameter(name = "label") @RequestParam String label) {
    ensureCourseExists(courseId);
    Section existing = findSectionInCourse(courseId, id);
    String normalizedSection = normalizeRequired("section", section);
    String normalizedLabel = normalizeRequired("label", label);

    Optional<Section> duplicate =
        sectionRepository.findByCourseIdAndSection(courseId, normalizedSection);
    if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Section %s already exists for course %d".formatted(normalizedSection, courseId));
    }

    existing.setSection(normalizedSection);
    existing.setLabel(normalizedLabel);
    return sectionRepository.save(existing);
  }

  /**
   * Delete a section from a course.
   *
   * @param courseId the id of the course
   * @param id the id of the section to delete
   * @return a message indicating the section was deleted
   */
  @Operation(summary = "Delete a section from a course")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @DeleteMapping("/{id}")
  public Object deleteSection(
      @Parameter(name = "courseId") @PathVariable Long courseId,
      @Parameter(name = "id") @PathVariable Long id) {
    ensureCourseExists(courseId);
    Section existing = findSectionInCourse(courseId, id);
    sectionRepository.delete(existing);
    return genericMessage("Section with id %d deleted".formatted(id));
  }

  private Course ensureCourseExists(Long courseId) {
    return courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
  }

  /**
   * Look up a section by id and verify that it belongs to the given course. A section that exists
   * but belongs to a different course is treated as not found, so that callers cannot read or
   * modify sections of courses they do not have access to.
   */
  private Section findSectionInCourse(Long courseId, Long id) {
    return sectionRepository
        .findById(id)
        .filter(s -> s.getCourse().getId().equals(courseId))
        .orElseThrow(() -> new EntityNotFoundException(Section.class, id));
  }

  private String normalizeRequired(String fieldName, String value) {
    String normalized = value.strip();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
    }
    return normalized;
  }
}
