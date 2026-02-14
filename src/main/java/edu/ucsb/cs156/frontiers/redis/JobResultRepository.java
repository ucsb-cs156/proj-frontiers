package edu.ucsb.cs156.frontiers.redis;

import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface JobResultRepository extends KeyValueRepository<JobResult, Long> {}
