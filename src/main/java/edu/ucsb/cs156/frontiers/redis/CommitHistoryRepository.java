package edu.ucsb.cs156.frontiers.redis;

import edu.ucsb.cs156.frontiers.models.CommitHistory;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface CommitHistoryRepository extends KeyValueRepository<CommitHistory, String> {}
