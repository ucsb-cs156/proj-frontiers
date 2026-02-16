package edu.ucsb.cs156.frontiers.mongo.repositories;

import edu.ucsb.cs156.frontiers.mongo.documents.CommitHistory;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommitHistoryRepository extends MongoRepository<CommitHistory, ObjectId> {
  Optional<CommitHistory> findByOwnerAndRepoAndBranch(String owner, String repo, String branch);
}
