package edu.ucsb.cs156.frontiers.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import edu.ucsb.cs156.frontiers.entities.User;

import javax.swing.text.html.Option;
import java.util.Optional;

/**
 * The UserRepository is a repository for User entities.
 */
@Repository
public interface UserRepository extends CrudRepository<User, Long> {
  /**
   * This method returns a User entity with a given email.
   * @param email email address of the user
   * @return Optional of User (empty if not found)
   */
  Optional<User> findByEmail(String email);

  Optional<User> findByGoogleSub(String googleSub);

  Optional<User> findByGithubLogin(String githubLogin);

  Optional<User> findByGithubId(int githubId);
}
