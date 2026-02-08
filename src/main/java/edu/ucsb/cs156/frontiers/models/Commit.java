package edu.ucsb.cs156.frontiers.models;

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
  private String message;
  private ZonedDateTime commitTime;
}
