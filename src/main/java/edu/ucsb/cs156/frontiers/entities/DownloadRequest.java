package edu.ucsb.cs156.frontiers.entities;


import edu.ucsb.cs156.frontiers.enums.DownloadRequestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "download_request")
public class DownloadRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JoinColumn(name = "course_id")
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Course course;

  @JoinColumn(name = "job_id")
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Job job;

  @Column(nullable = false)
  @NotNull
  @Enumerated(EnumType.STRING)
  private DownloadRequestType downloadType;

  @NotBlank @NotNull private String org;

  @NotBlank @NotNull private String repo;

  @NotBlank @NotNull @Builder.Default private String branch = "main";

  @Column(nullable = true)
  private Instant startDate;

  @Column(nullable = true)
  private Instant endDate;
}
