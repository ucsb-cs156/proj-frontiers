package edu.ucsb.cs156.frontiers.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    name = "assignment",
    uniqueConstraints = @UniqueConstraint(columnNames = {"name", "course_id"}))
public class Assignment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "course_id", nullable = false)
  @JsonIgnore
  @ToString.Exclude
  private Course course;

  @Column(nullable = false)
  @NotBlank
  private String name;

  @Column(nullable = false)
  @NotBlank
  private String asnType; // "individual" or "team"

  @Column(nullable = false)
  @NotBlank
  private String visibility; // "public" or "private"

  @Column(nullable = false)
  @NotBlank
  private String permission; // "read", "write", "maintain", "admin"
}
