package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.Assignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
  List<Assignment> findByCourseId(Long courseId);

  List<Assignment> findByCourseIdOrderByNameAsc(Long courseId);

  Optional<Assignment> findByCourseIdAndName(Long courseId, String name);
}
