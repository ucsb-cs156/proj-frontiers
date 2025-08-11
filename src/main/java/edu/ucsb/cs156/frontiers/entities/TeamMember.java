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
    name = "team_member",
    uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "roster_student_id"}))
public class TeamMember {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "roster_student_id")
  private RosterStudent rosterStudent;

  @ManyToOne
  @JoinColumn(name = "team_id")
  @JsonIgnore
  @ToString.Exclude
  private Team team;
}
