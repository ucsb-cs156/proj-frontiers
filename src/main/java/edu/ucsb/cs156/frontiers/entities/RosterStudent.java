package edu.ucsb.cs156.frontiers.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
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

    @ManyToOne
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    @Enumerated(EnumType.STRING)
    private RosterStatus rosterStatus;

    @Enumerated(EnumType.STRING)
    private OrgStatus orgStatus;

    private int githubId;
    private String githubLogin;

}
