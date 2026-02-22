package edu.ucsb.cs156.frontiers.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;

/** DTO for {@link edu.ucsb.cs156.frontiers.entities.Commit} */
public record CommitDto(
    Long id,
    @JsonProperty("org") String branchIdOrg,
    @JsonProperty("repo") String branchIdRepo,
    @JsonProperty("branch") String branchIdBranchName,
    String sha,
    String url,
    String message,
    Instant commitTime,
    String committerName,
    String committerEmail,
    String committerLogin,
    String authorName,
    String authorEmail,
    String authorLogin)
    implements Serializable {}
