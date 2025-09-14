package edu.ucsb.cs156.frontiers.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Section;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public class SectionRepositoryTests {

  @Autowired private CourseRepository courseRepository;
  @Autowired private SectionRepository sectionRepository;

  private Course createTestCourse(String name) {
    Course course = Course.builder().courseName(name).term("Spring 2024").school("UCSB").build();
    return courseRepository.save(course);
  }

  private Section createTestSection(Course course, String section, String label) {
    Section s = Section.builder().course(course).section(section).label(label).build();
    return sectionRepository.save(s);
  }

  @Test
  void canCreateSection() {
    // Arrange
    Course course = createTestCourse("CMPSC 156");

    // Act
    Section section = createTestSection(course, "0101", "Lecture 1");

    // Assert
    assertEquals(course.getId(), section.getCourse().getId());
    assertEquals("0101", section.getSection());
    assertEquals("Lecture 1", section.getLabel());
  }

  @Test
  void canFindSectionsByCourseId() {
    // Arrange
    Course course1 = createTestCourse("CMPSC 156");
    Course course2 = createTestCourse("CMPSC 174A");

    createTestSection(course1, "0101", "Lecture 1");
    createTestSection(course1, "0102", "Lecture 2");
    createTestSection(course2, "0201", "Lecture 1");

    // Act
    List<Section> course1Sections = sectionRepository.findByCourseId(course1.getId());
    List<Section> course2Sections = sectionRepository.findByCourseId(course2.getId());

    // Assert
    assertEquals(2, course1Sections.size());
    assertEquals(1, course2Sections.size());

    assertEquals("0101", course1Sections.get(0).getSection());
    assertEquals("0102", course1Sections.get(1).getSection());
    assertEquals("0201", course2Sections.get(0).getSection());
  }

  @Test
  void testSectionRequiredFields() {
    // Arrange
    Course course = createTestCourse("CMPSC 156");

    // Act & Assert - test that all required fields are properly populated
    Section section =
        Section.builder().course(course).section("0101").label("Lecture Section A").build();

    Section savedSection = sectionRepository.save(section);

    assertEquals(course.getId(), savedSection.getCourse().getId());
    assertEquals("0101", savedSection.getSection());
    assertEquals("Lecture Section A", savedSection.getLabel());
  }
}
