package edu.ucsb.cs156.frontiers.entities;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    name = "commits",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"org", "repo", "branch", "sha"})},
    indexes = {@Index(name = "idx_commit_filtering", columnList = "org, repo, branch, commitTime")})
public class Commit {

  @Id
  @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
  private Long id;

  @JoinColumns(
      value = {
        @JoinColumn(name = "org", referencedColumnName = "org"),
        @JoinColumn(name = "repo", referencedColumnName = "repo"),
        @JoinColumn(name = "branch", referencedColumnName = "branchName"),
      },
      foreignKey = @ForeignKey(name = "fk_commits_branch"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  @ManyToOne
  @NotNull
  private Branch branch;

  @JsonAlias("oid")
  @NotNull
  private String sha;

  private String url;

  @JsonAlias("messageHeadline")
  private String message;

  @JsonAlias("committedDate")
  private Instant commitTime;

  private String committerName;
  private String committerEmail;
  private String committerLogin;
  private String authorName;
  private String authorEmail;
  private String authorLogin;
  private boolean isMergeCommit;

  @JsonAlias("author")
  public void setAuthor(JsonNode node) {
    if (node == null) return;
    this.authorName = node.path("name").asText();
    this.authorEmail = node.path("email").asText();
    this.authorLogin = node.path("user").path("login").asText();
  }

  @JsonAlias("committer")
  public void setCommitter(JsonNode node) {
    if (node == null) return;
    this.committerName = node.path("name").asText();
    this.committerEmail = node.path("email").asText();
    this.committerLogin = node.path("user").path("login").asText();
  }

  @JsonAlias("parents")
  public void setIsMergeCommit(JsonNode node) {
    if (node == null) return;
    this.isMergeCommit = node.path("totalCount").asInt() > 1;
  }
}
