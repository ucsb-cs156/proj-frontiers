package edu.ucsb.cs156.frontiers.models;

import java.time.ZonedDateTime;
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
  private String count;
  private ZonedDateTime retrievedTime;
}
