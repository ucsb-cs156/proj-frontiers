package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.Assignment;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository extends CrudRepository<Assignment, Long> {
  List<Assignment> findByCourseId(Long courseId);
}
