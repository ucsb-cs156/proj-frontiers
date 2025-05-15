package edu.ucsb.cs156.frontiers.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.ucsb.cs156.frontiers.entities.Instructor;

public interface InstructorRepository extends JpaRepository<Instructor, String> {
}
