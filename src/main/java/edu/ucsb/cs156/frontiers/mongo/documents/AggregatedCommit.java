package edu.ucsb.cs156.frontiers.mongo.documents;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "aggregated_commits")
@CompoundIndex(
    name = "session_branch_sha_idx",
    def = "{'sessionId': 1, 'parentBranch': 1, 'sha': 1}",
    unique = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AggregatedCommit {
  @Id private ObjectId id;

  @Indexed private ObjectId sessionId;

  private ObjectId parentBranch;

  private String url;

  @JsonAlias("oid")
  private String sha;

  @JsonAlias("messageHeadline")
  private String message;

  @JsonAlias("committedDate")
  @Indexed
  private Instant commitTime;

  private String committerName;
  private String committerEmail;
  private String committerLogin;
  private String authorName;
  private String authorEmail;
  private String authorLogin;
}
