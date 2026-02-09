package edu.ucsb.cs156.frontiers.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Commit {
  private String url;

  @JsonProperty("messageHeadline")
  private String message;

  @JsonProperty("committedDate")
  private ZonedDateTime commitTime;

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
