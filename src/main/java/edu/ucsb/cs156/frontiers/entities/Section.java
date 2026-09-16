package edu.ucsb.cs156.frontiers.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    name = "sections",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_SECTIONS_COURSE_SECTION",
          columnNames = {"course_id", "section"})
    })
public class Section {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "course_id", nullable = false)
  @JsonIgnore
  @ToString.Exclude
  private Course course;

  @Column(nullable = false)
  private String section;

  @Column(nullable = false)
  private String label;
}
