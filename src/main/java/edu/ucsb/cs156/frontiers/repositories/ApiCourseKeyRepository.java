package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.ApiCourseKey;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiCourseKeyRepository extends CrudRepository<ApiCourseKey, Long> {
  List<ApiCourseKey> findByCourseIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
      Long courseId, ZonedDateTime now);

  Optional<ApiCourseKey> findByIdAndCourseId(Long id, Long courseId);
}
