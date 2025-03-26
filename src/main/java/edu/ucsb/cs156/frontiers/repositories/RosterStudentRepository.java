package edu.ucsb.cs156.frontiers.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;

public interface RosterStudentRepository extends JpaRepository<RosterStudent, Long>
{
    public Iterable<RosterStudent> findByCourseId(Long courseId);
    public Optional<RosterStudent> findByCourseIdAndStudentId(Long courseId, String studentId);
}
