package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.Section;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, Long> {
  List<Section> findByCourseId(Long courseId);

  List<Section> findByCourseIdOrderBySectionAsc(Long courseId);

  Optional<Section> findByCourseIdAndSection(Long courseId, String section);
}
