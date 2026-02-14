package edu.ucsb.cs156.frontiers.redis;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder
@EqualsAndHashCode
public class JobResultDTO {
  private Long jobId;
  private Object result;
}
