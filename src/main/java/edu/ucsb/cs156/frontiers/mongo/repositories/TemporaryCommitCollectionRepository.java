package edu.ucsb.cs156.frontiers.mongo.repositories;

import edu.ucsb.cs156.frontiers.mongo.documents.TemporaryCommitCollection;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TemporaryCommitCollectionRepository
    extends MongoRepository<TemporaryCommitCollection, ObjectId>,
        TemporaryCommitCollectionAggregator {

  TemporaryCommitCollection findBySessionId(String sessionId);
}
