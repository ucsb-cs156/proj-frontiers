package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.RateLimitDataPoint;
import org.springframework.data.repository.CrudRepository;

public interface RateLimitDataPointRepository extends CrudRepository<RateLimitDataPoint, Long> {}
