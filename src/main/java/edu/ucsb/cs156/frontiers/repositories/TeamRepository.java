package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Iterable<Team> findByCourseId(Long courseId);
}