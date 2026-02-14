package edu.ucsb.cs156.frontiers.redis;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "job_result", timeToLive = 7 * 24 * 60 * 60)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.WRAPPER_ARRAY)
public abstract class JobResult {
  @Id private Long jobId;

  public abstract Object getResult();

  public JobResultDTO toDTO() {
    return JobResultDTO.builder().jobId(jobId).result(getResult()).build();
  }
}
