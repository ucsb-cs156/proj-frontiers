package edu.ucsb.cs156.frontiers.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;

import java.util.List;

public interface RosterStudentRepository extends JpaRepository<RosterStudent, Long>
{
    List<RosterStudent> findAllByEmail(String email);
}
