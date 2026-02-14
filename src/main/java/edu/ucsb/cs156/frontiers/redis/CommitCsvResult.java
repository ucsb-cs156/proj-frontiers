package edu.ucsb.cs156.frontiers.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitCsvResult extends JobResult {

  @Getter
  @Builder
  public static class JobData {
    private byte[] csvData;
  }

  private JobData jobData;

  @Override
  public Object getResult() {
    return jobData;
  }
}
