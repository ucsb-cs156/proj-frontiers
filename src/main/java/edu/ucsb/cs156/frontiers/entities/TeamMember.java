package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.*;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class TeamMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="roster_student_id")
    private RosterStudent rosterStudent;

    @ManyToOne
    @JoinColumn(name = "team_id")
    @JsonIgnore
    @ToString.Exclude
    private Team team;
}