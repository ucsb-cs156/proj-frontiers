package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.CourseOption;
import edu.ucsb.cs156.frontiers.entities.Section;
import edu.ucsb.cs156.frontiers.enums.CourseOptions;
import edu.ucsb.cs156.frontiers.repositories.CourseOptionRepository;
import edu.ucsb.cs156.frontiers.repositories.SectionRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Centralizes the logic for checking whether the TRANSLATE_SECTIONS course option is enabled, and
 * for translating a raw roster student section value into its human readable label. This is the
 * single place where this logic lives, so that it behaves consistently everywhere a section value
 * is displayed or exported (e.g. the CATME CSV export, the roster student tables).
 */
@Service
public class SectionTranslationService {

  @Autowired private CourseOptionRepository courseOptionRepository;

  @Autowired private SectionRepository sectionRepository;

  /**
   * Returns the map from raw section value to label for the course, if the TRANSLATE_SECTIONS
   * course option is enabled; otherwise an empty map (raw values are used as-is).
   *
   * @param courseId the id of the course
   * @return map of raw section value to translated label
   */
  public Map<String, String> getSectionTranslations(Long courseId) {
    Map<String, String> translations = new HashMap<>();
    if (isTranslateSectionsEnabled(courseId)) {
      for (Section section : sectionRepository.findByCourseId(courseId)) {
        translations.put(section.getSection(), section.getLabel());
      }
    }
    return translations;
  }

  /**
   * Returns whether the TRANSLATE_SECTIONS course option is enabled for the given course.
   *
   * @param courseId the id of the course
   * @return true if the TRANSLATE_SECTIONS course option is enabled, false otherwise
   */
  public boolean isTranslateSectionsEnabled(Long courseId) {
    return courseOptionRepository
        .findByCourseIdAndOption(courseId, CourseOptions.TRANSLATE_SECTIONS.name())
        .map(CourseOption::getEnabled)
        .orElse(false);
  }

  /**
   * Translates a raw roster student section value using the given map. A null section is treated
   * as blank; a section with no translation is returned unchanged.
   *
   * @param rawSection the section value from the roster student row (may be null)
   * @param translations map of raw section value to label
   * @return the translated section value
   */
  public static String translateSection(String rawSection, Map<String, String> translations) {
    String section = rawSection == null ? "" : rawSection;
    return translations.getOrDefault(section, section);
  }
}
