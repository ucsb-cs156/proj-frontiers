package edu.ucsb.cs156.frontiers.models;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommitHistory {

  private String owner;
  private String repo;
  private Integer count;
  private ZonedDateTime retrievedTime;
  @Builder.Default private List<Commit> commits = new ArrayList<>();
}
