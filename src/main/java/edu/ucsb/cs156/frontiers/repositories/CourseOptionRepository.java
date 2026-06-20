package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.CourseOption;
import edu.ucsb.cs156.frontiers.entities.CourseOptionKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseOptionRepository extends JpaRepository<CourseOption, CourseOptionKey> {
  List<CourseOption> findByCourseId(Long courseId);

  Optional<CourseOption> findByCourseIdAndOption(Long courseId, String option);
}
