package edu.ucsb.cs156.frontiers.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import jakarta.persistence.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_ROSTER_STUDENT_COURSE_STUDENT",
          columnNames = {"course_id", "student_id"}),
      @UniqueConstraint(
          name = "UK_ROSTER_STUDENT_COURSE_EMAIL",
          columnNames = {"course_id", "email"})
    })
// @EqualsAndHashCode(exclude = {"teamMembers"})
public class RosterStudent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "course_id")
  private Course course;

  private String studentId;
  private String firstName;
  private String lastName;
  private String email;
  @Builder.Default private String section = "";

  @ManyToOne
  @JoinColumn(name = "user_id")
  @ToString.Exclude
  private User user;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "rosterStudent")
  @Fetch(FetchMode.JOIN)
  @JsonIgnore
  @ToString.Exclude
  private List<TeamMember> teamMembers;

  @Enumerated(EnumType.STRING)
  private RosterStatus rosterStatus;

  @Enumerated(EnumType.STRING)
  private OrgStatus orgStatus;

  private Integer githubId;
  private String githubLogin;

  public List<String> getTeams() {
    if (teamMembers == null) {
      return List.of();
    } else {
      return teamMembers.stream().map(tm -> tm.getTeam().getName()).toList();
    }
  }
}
