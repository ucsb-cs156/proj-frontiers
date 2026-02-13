package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.Instructor;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/** The InstructorRepository is a repository for Instructor entities. */
@Repository
public interface InstructorRepository extends CrudRepository<Instructor, String> {
  /**
   * This method returns an Instructor entity with a given email.
   *
   * @param email email address of the instructor
   * @return Optional of Instructor (empty if not found)
   */
  @Cacheable("instructors")
  Optional<Instructor> findByEmail(String email);

  @Cacheable("instructors")
  boolean existsByEmail(String email);
}
