package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RosterStudentRepository extends JpaRepository<RosterStudent, Long> {
  List<RosterStudent> findAllByEmail(String email);

  public Iterable<RosterStudent> findByCourseId(Long courseId);

  Page<RosterStudent> findByCourseId(Long courseId, Pageable pageable);

  public Optional<RosterStudent> findByCourseIdAndStudentId(Long courseId, String studentId);

  public Optional<RosterStudent> findByCourseIdAndEmail(Long courseId, String email);

  Optional<RosterStudent> findByCourseAndGithubId(Course course, int githubId);

  Optional<RosterStudent> findByCourseAndGithubLogin(Course course, String githubLogin);

  Iterable<RosterStudent> findAllByUser(User user);
}
