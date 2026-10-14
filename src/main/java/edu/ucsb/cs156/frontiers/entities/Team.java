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
@EqualsAndHashCode(exclude = {"teamMembers"})
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

  @Column(nullable = true)
  private Integer githubTeamId;

  @Column(nullable = true)
  private String githubTeamSlug;

  private Integer canvasId;

  /**
   * A staff team stands in for a student team so that the course staff can try out team-based
   * features (team repositories, autograders) themselves. Its members are the course staff, not
   * roster students: it has no team members of its own, is pushed to GitHub with the staff as its
   * members, and is left out when teams are pushed to Canvas.
   */
  @Column(nullable = false)
  @Builder.Default
  private Boolean isStaffTeam = false;
}
