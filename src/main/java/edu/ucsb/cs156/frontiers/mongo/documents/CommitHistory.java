package edu.ucsb.cs156.frontiers.mongo.documents;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "commit_history")
@CompoundIndexes({
  @CompoundIndex(
      name = "org_repo_branch_idx",
      def = "{'org': 1, 'repo': 1, 'branch': 1}",
      unique = true)
})
public class CommitHistory {
  @MongoId private ObjectId id;
  private String owner;
  private String repo;
  private String branch;
  private Instant retrievedTime;
  private String headSha;
}
