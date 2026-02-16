package edu.ucsb.cs156.frontiers.mongo.documents;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "commits")
public class Commit {

  @MongoId private ObjectId id;

  @Indexed private ObjectId parentBranch;

  private String url;

  @JsonAlias("oid")
  private String sha;

  @JsonProperty("messageHeadline")
  private String message;

  @JsonProperty("committedDate")
  private Instant commitTime;

  private String committerName;
  private String committerEmail;
  private String committerLogin;
  private String authorName;
  private String authorEmail;
  private String authorLogin;

  @JsonProperty("author")
  public void setAuthor(JsonNode node) {
    if (node == null) return;
    this.authorName = node.path("name").asText();
    this.authorEmail = node.path("email").asText();
    this.authorLogin = node.path("user").path("login").asText();
  }

  @JsonProperty("committer")
  public void setCommitter(JsonNode node) {
    if (node == null) return;
    this.committerName = node.path("name").asText();
    this.committerEmail = node.path("email").asText();
    this.committerLogin = node.path("user").path("login").asText();
  }
}
