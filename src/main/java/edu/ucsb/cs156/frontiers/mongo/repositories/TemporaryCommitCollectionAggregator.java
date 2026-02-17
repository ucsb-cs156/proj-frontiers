package edu.ucsb.cs156.frontiers.mongo.repositories;

import edu.ucsb.cs156.frontiers.models.Branch;
import edu.ucsb.cs156.frontiers.mongo.documents.TemporaryCommitCollection;
import java.util.List;

public interface TemporaryCommitCollectionAggregator {

  TemporaryCommitCollection createSession(TemporaryCommitCollection starter, List<Branch> branches);
}
