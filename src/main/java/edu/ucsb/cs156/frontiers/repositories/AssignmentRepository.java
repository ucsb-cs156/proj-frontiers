package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {}
