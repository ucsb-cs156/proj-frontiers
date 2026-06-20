package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "COURSE_OPTION")
@IdClass(CourseOptionKey.class)
public class CourseOption {

  @Id
  @Column(name = "COURSE", nullable = false)
  private Long courseId;

  @Id
  @Column(name = "OPTION", nullable = false)
  private String option;

  @Column(name = "ENABLED", nullable = false)
  private boolean enabled;
}
