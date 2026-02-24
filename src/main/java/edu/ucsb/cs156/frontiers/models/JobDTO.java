package edu.ucsb.cs156.frontiers.models;

import edu.ucsb.cs156.frontiers.entities.Job;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobDTO {
  private long id;
  private String status;
  private String jobName;
  private String userEmail;
  private String courseName;
  private ZonedDateTime createdAt;
  private ZonedDateTime updatedAt;

  public static JobDTO fromEntity(Job job) {
    return JobDTO.builder()
        .id(job.getId())
        .status(job.getStatus())
        .jobName(job.getJobName())
        .createdAt(job.getCreatedAt())
        .updatedAt(job.getUpdatedAt())
        .userEmail(job.getCreatedBy() != null ? job.getCreatedBy().getEmail() : null)
        .courseName(job.getCourse() != null ? job.getCourse().getCourseName() : null)
        .build();
  }
}
