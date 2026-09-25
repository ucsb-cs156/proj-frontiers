package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.Assignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
  /**
   * The assignments of a course, sorted by repository prefix.
   *
   * @param courseId the id of the course
   * @return the assignments, empty if there are none
   */
  List<Assignment> findByCourseIdOrderByRepoPrefixAsc(Long courseId);
}
