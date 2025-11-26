package edu.ucsb.cs156.frontiers.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ucsb.cs156.frontiers.entities.Assignment;
import edu.ucsb.cs156.frontiers.entities.Course;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public class AssignmentRepositoryTests {

  @Autowired private CourseRepository courseRepository;
  @Autowired private AssignmentRepository assignmentRepository;

  private Course createTestCourse(String name) {
    Course course = Course.builder().courseName(name).term("Spring 2024").school("UCSB").build();
    return courseRepository.save(course);
  }

  private Assignment createTestAssignment(Course course, String name) {
    Assignment a =
        Assignment.builder()
            .course(course)
            .name(name)
            .asnType("individual")
            .visibility("public")
            .permission("maintain")
            .build();
    return assignmentRepository.save(a);
  }

  @Test
  void canCreateAssignment() {
    // Arrange
    Course course = createTestCourse("CMPSC 156");

    // Act
    Assignment assignment = createTestAssignment(course, "jpa00");

    // Assert
    assertEquals(course.getId(), assignment.getCourse().getId());
    assertEquals("jpa00", assignment.getName());
    assertEquals("individual", assignment.getAsnType());
    assertEquals("public", assignment.getVisibility());
    assertEquals("maintain", assignment.getPermission());
  }

  @Test
  void canFindAssignmentsByCourseId() {
    // Arrange
    Course course1 = createTestCourse("CMPSC 156");
    Course course2 = createTestCourse("CMPSC 174A");

    createTestAssignment(course1, "jpa01");
    createTestAssignment(course1, "jpa02");
    createTestAssignment(course1, "jpa03");

    // Act
    List<Assignment> course1Assignments = assignmentRepository.findByCourseId(course1.getId());
    List<Assignment> course2Assignments = assignmentRepository.findByCourseId(course2.getId());

    // Assert
    assertEquals(3, course1Assignments.size());
    assertEquals(0, course2Assignments.size());

    assertEquals("jpa01", course1Assignments.get(0).getName());
    assertEquals("jpa02", course1Assignments.get(1).getName());
    assertEquals("jpa03", course1Assignments.get(2).getName());
  }

  @Test
  void testAssignmentRequiredFields() {
    // Arrange
    Course course = createTestCourse("CMPSC 156");

    // Act & Assert - test that all required fields are properly populated
    Assignment assignment =
        Assignment.builder()
            .course(course)
            .name("jpa00")
            .asnType("individual")
            .visibility("public")
            .permission("maintain")
            .build();

    Assignment savedAssignment = assignmentRepository.save(assignment);

    assertEquals(course.getId(), savedAssignment.getCourse().getId());
    assertEquals("jpa00", savedAssignment.getName());
    assertEquals("individual", savedAssignment.getAsnType());
    assertEquals("public", savedAssignment.getVisibility());
    assertEquals("maintain", savedAssignment.getPermission());
  }
}
