package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.Admin;
import java.util.Optional;
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
  Optional<Admin> findByEmail(String email);

  boolean existsByEmail(String email);
}
