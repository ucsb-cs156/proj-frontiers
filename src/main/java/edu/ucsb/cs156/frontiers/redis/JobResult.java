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
@RedisHash("job_result")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.WRAPPER_ARRAY)
public abstract class JobResult {
  @Id private Long jobId;

  public abstract Object getResult();

  public JobResultDTO toDTO() {
    return JobResultDTO.builder().jobId(jobId).result(getResult()).build();
  }
}
