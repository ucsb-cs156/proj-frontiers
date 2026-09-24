package edu.ucsb.cs156.frontiers.controllers;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.DokkuAccountTranslation;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.DokkuAccountTranslationRepository;
import edu.ucsb.cs156.frontiers.utilities.CanonicalFormConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * CRUD for {@link DokkuAccountTranslation}: the table of email addresses whose dokku username is
 * not simply the part of the email before the {@code @}.
 *
 * <p>The table is global (not per-course), but it is managed from the Dokku tab of a course, so
 * every endpoint takes a {@code courseId} that is used only to check that the caller can manage
 * that course.
 */
@Tag(name = "Dokku")
@RequestMapping("/api/dokku/translations")
@RestController
public class DokkuAccountTranslationsController extends ApiController {

  @Autowired private DokkuAccountTranslationRepository dokkuAccountTranslationRepository;

  @Autowired private CourseRepository courseRepository;

  @Operation(summary = "List all dokku account translations, sorted by email")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("")
  public List<DokkuAccountTranslation> getTranslations(
      @Parameter(name = "courseId") @RequestParam Long courseId) {
    ensureCourseExists(courseId);
    return dokkuAccountTranslationRepository.findAllByOrderByEmailAsc();
  }

  @Operation(summary = "Create a dokku account translation")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("")
  public DokkuAccountTranslation postTranslation(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "email") @RequestParam String email,
      @Parameter(name = "username") @RequestParam String username) {
    ensureCourseExists(courseId);
    String normalizedEmail = normalizeEmail(email);
    String normalizedUsername = normalizeRequired("username", username);

    if (dokkuAccountTranslationRepository.findById(normalizedEmail).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Dokku account translation for %s already exists".formatted(normalizedEmail));
    }

    DokkuAccountTranslation translation =
        DokkuAccountTranslation.builder()
            .email(normalizedEmail)
            .username(normalizedUsername)
            .build();
    return dokkuAccountTranslationRepository.save(translation);
  }

  @Operation(summary = "Update the username of a dokku account translation")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PutMapping("")
  public DokkuAccountTranslation putTranslation(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "email") @RequestParam String email,
      @Parameter(name = "username") @RequestParam String username) {
    ensureCourseExists(courseId);
    String normalizedEmail = normalizeEmail(email);
    String normalizedUsername = normalizeRequired("username", username);

    DokkuAccountTranslation existing = findTranslation(normalizedEmail);
    existing.setUsername(normalizedUsername);
    return dokkuAccountTranslationRepository.save(existing);
  }

  @Operation(summary = "Delete a dokku account translation")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @DeleteMapping("")
  public Object deleteTranslation(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "email") @RequestParam String email) {
    ensureCourseExists(courseId);
    String normalizedEmail = normalizeEmail(email);

    DokkuAccountTranslation existing = findTranslation(normalizedEmail);
    dokkuAccountTranslationRepository.delete(existing);
    return genericMessage("Dokku account translation for %s deleted".formatted(normalizedEmail));
  }

  private void ensureCourseExists(Long courseId) {
    courseRepository
        .findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
  }

  private DokkuAccountTranslation findTranslation(String email) {
    return dokkuAccountTranslationRepository
        .findById(email)
        .orElseThrow(() -> new EntityNotFoundException(DokkuAccountTranslation.class, email));
  }

  /** Strips and canonicalizes an email; a blank email is a 400. */
  private String normalizeEmail(String email) {
    return CanonicalFormConverter.convertToValidEmail(normalizeRequired("email", email));
  }

  private String normalizeRequired(String fieldName, String value) {
    String normalized = value.strip();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
    }
    return normalized;
  }
}
