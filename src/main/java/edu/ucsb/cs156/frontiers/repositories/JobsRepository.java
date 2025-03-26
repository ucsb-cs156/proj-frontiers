package edu.ucsb.cs156.frontiers.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import edu.ucsb.cs156.frontiers.entities.Job;

@Repository
public interface JobsRepository extends CrudRepository<Job, Long> {}
