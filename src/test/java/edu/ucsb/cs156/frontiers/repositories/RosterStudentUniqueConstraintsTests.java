package edu.ucsb.cs156.frontiers.repositories;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
public class RosterStudentUniqueConstraintsTests {

  @Autowired private CourseRepository courseRepository;
  @Autowired private RosterStudentRepository rosterStudentRepository;

  private Course newCourse(String name) {
    Course c = Course.builder().courseName(name).build();
    return courseRepository.save(c);
  }

  private RosterStudent rs(Course c, String studentId, String email) {
    return RosterStudent.builder().course(c).studentId(studentId).email(email).build();
  }

  @Test
  void cannotHaveDuplicateStudentIdWithinSameCourse() {
    Course c = newCourse("A");

    rosterStudentRepository.saveAndFlush(rs(c, "123", "a@example.org"));

    assertThrows(
        DataIntegrityViolationException.class,
        () -> rosterStudentRepository.saveAndFlush(rs(c, "123", "b@example.org")));
  }

  @Test
  void cannotHaveDuplicateEmailWithinSameCourse() {
    Course c = newCourse("B");

    rosterStudentRepository.saveAndFlush(rs(c, "111", "dup@example.org"));

    assertThrows(
        DataIntegrityViolationException.class,
        () -> rosterStudentRepository.saveAndFlush(rs(c, "222", "dup@example.org")));
  }

  @Test
  void sameStudentIdAllowedAcrossDifferentCourses() {
    Course c1 = newCourse("C1");
    Course c2 = newCourse("C2");

    assertDoesNotThrow(
        () -> {
          rosterStudentRepository.saveAndFlush(rs(c1, "321", "c1@example.org"));
          rosterStudentRepository.saveAndFlush(rs(c2, "321", "c2@example.org"));
        });
  }

  @Test
  void sameEmailAllowedAcrossDifferentCourses() {
    Course c1 = newCourse("D1");
    Course c2 = newCourse("D2");

    assertDoesNotThrow(
        () -> {
          rosterStudentRepository.saveAndFlush(rs(c1, "555", "same@example.org"));
          rosterStudentRepository.saveAndFlush(rs(c2, "666", "same@example.org"));
        });
  }
}
