package edu.ucsb.cs156.frontiers.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "team", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "course_id"}))
public class Team {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  @NotBlank
  private String name;

  @ManyToOne
  @JoinColumn(name = "course_id")
  @JsonIgnore
  @ToString.Exclude
  private Course course;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "team")
  @Fetch(FetchMode.JOIN)
  private List<TeamMember> teamMembers;
}
