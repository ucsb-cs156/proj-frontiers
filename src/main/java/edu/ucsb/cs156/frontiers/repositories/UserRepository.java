package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/** The UserRepository is a repository for User entities. */
@Repository
public interface UserRepository extends CrudRepository<User, Long> {
  /**
   * This method returns a User entity with a given email.
   *
   * @param email email address of the user
   * @return Optional of User (empty if not found)
   */
  Optional<User> findByEmail(String email);

  Page<User> findAll(Pageable pageable);

  Optional<User> findByGoogleSub(String googleSub);

  Optional<User> findByGithubLogin(String githubLogin);

  Optional<User> findByGithubId(int githubId);
}
