package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.Instructor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Instructor entities.
 * Provides CRUD operations for the Instructor entity using email as the primary key.
 */
@Repository
public interface InstructorRepository extends CrudRepository<Instructor, String> {

}