package edu.ucsb.cs156.frontiers.mongo.repositories;

import edu.ucsb.cs156.frontiers.mongo.documents.Commit;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommitRepository extends MongoRepository<Commit, ObjectId> {

  List<Commit> findByParentBranch(ObjectId parentBranch);

  Page<Commit> findByParentBranch(ObjectId parentBranch, Pageable pageable);
}
