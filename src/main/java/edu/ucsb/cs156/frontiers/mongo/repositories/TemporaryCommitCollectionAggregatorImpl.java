package edu.ucsb.cs156.frontiers.mongo.repositories;

import edu.ucsb.cs156.frontiers.models.Branch;
import edu.ucsb.cs156.frontiers.mongo.documents.AggregatedCommit;
import edu.ucsb.cs156.frontiers.mongo.documents.TemporaryCommitCollection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.MergeOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TemporaryCommitCollectionAggregatorImpl
    implements TemporaryCommitCollectionAggregator {

  private final MongoOperations mongoOperations;

  @Override
  public TemporaryCommitCollection createSession(
      TemporaryCommitCollection starter, List<Branch> branches) {
    assert (branches != null && !branches.isEmpty());
    System.out.println("starter = " + starter);
    mongoOperations.save(starter);
    System.out.println("gets this far");

    List<Criteria> branchCriteria =
        branches.stream()
            .map(
                b ->
                    Criteria.where("owner")
                        .is(b.owner())
                        .and("repo")
                        .is(b.repo())
                        .and("branch")
                        .is(b.branch()))
            .toList();

    Query query = new Query(new Criteria().orOperator(branchCriteria.toArray(new Criteria[0])));
    query.fields().include("_id");

    List<Document> docs = mongoOperations.find(query, Document.class, "commit_history");
    List<ObjectId> ids =
        docs.stream().map(doc -> doc.getObjectId("_id")).collect(Collectors.toList());
    Aggregation agg =
        Aggregation.newAggregation(
            Aggregation.match(Criteria.where("parentBranch").in(ids)),
            Aggregation.project()
                .andExpression("{$toObjectId: '" + starter.getId().toHexString() + "'}")
                .as("sessionId")
                .and("sha")
                .as("sha")
                .and("message")
                .as("message")
                .and("commitTime")
                .as("commitTime")
                .and("committerName")
                .as("committerName")
                .and("committerEmail")
                .as("committerEmail")
                .and("committerLogin")
                .as("committerLogin")
                .and("authorName")
                .as("authorName")
                .and("authorEmail")
                .as("authorEmail")
                .and("authorLogin")
                .as("authorLogin")
                .and("parentBranch")
                .as("parentBranch")
                .and("url")
                .as("url"),
            Aggregation.sort(Sort.by(Sort.Direction.DESC, "commitTime")),
            MergeOperation.mergeInto("aggregated_commits"));

    mongoOperations.aggregate(agg, "commits", AggregatedCommit.class);

    return starter;
  }
}
