package edu.ucsb.cs156.frontiers.models;

import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@NoArgsConstructor
@RedisHash(value = "commit_history", timeToLive = 7 * 24 * 60 * 60)
public class CommitHistory {

  @NotNull private String owner;
  @NotNull private String repo;
  @NotNull private String branch;
  private Integer count;
  private ZonedDateTime retrievedTime;
  private String commitUrl;
  private List<Commit> commits = new ArrayList<>();

  @Id
  @Setter(AccessLevel.NONE)
  private String id;

  @Builder
  private CommitHistory(
      String owner,
      String repo,
      String branch,
      Integer count,
      ZonedDateTime retrievedTime,
      String commitUrl,
      List<Commit> commits) {
    this.owner = owner;
    this.repo = repo;
    this.branch = branch;
    this.count = count;
    this.retrievedTime = retrievedTime;
    this.commitUrl = commitUrl;
    this.commits = commits != null ? commits : new ArrayList<>();
    this.id = getId(owner, repo, branch);
  }

  public static String getId(String owner, String repo, String branch) {
    return owner + ":" + repo + ":" + branch;
  }
}
