package edu.ucsb.cs156.frontiers.mongo.repositories;

import edu.ucsb.cs156.frontiers.mongo.documents.AggregatedCommit;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AggregatedCommitRepository extends MongoRepository<AggregatedCommit, String> {

  Page<AggregatedCommit> findAllBySessionId(ObjectId sessionId, Pageable pageable);
}
