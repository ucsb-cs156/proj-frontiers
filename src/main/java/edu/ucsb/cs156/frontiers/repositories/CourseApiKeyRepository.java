package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.CourseApiKey;
import edu.ucsb.cs156.frontiers.entities.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface CourseApiKeyRepository extends CrudRepository<CourseApiKey, Long> {

  Optional<CourseApiKey> findByKeyHash(String key);

  List<CourseApiKey> findByCourseId(Long courseId);

  List<CourseApiKey> findByCreatedBy(User createdBy);
}
