package edu.ucsb.cs156.example.repositories;

import edu.ucsb.cs156.example.entities.Job;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobsRepository extends CrudRepository<Job, Long> {}
