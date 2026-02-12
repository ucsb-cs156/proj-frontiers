package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.Admin;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends CrudRepository<Admin, String> {
  /**
   * This method returns an Admin entity with a given email.
   *
   * @param email email address of the admin
   * @return Optional of Admin (empty if not found)
   */
  @Cacheable("admins")
  Optional<Admin> findByEmail(String email);

  @Cacheable("admins")
  boolean existsByEmail(String email);
}
