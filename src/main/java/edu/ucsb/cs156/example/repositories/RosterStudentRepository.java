package edu.ucsb.cs156.example.repositories;

import edu.ucsb.cs156.example.entities.RosterStudent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RosterStudentRepository extends JpaRepository<RosterStudent, Long>
{
}
