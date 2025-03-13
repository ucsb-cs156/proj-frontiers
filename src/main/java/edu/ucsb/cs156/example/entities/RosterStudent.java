package edu.ucsb.cs156.example.entities;

import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import edu.ucsb.cs156.example.services.statuses.OrgStatus;
import edu.ucsb.cs156.example.services.statuses.RosterStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @JoinColumn(name = "roster_id")
    private Roster roster;
    private String perm;
    private String firstName;
    private String lastName;
    private String email;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private RosterStatus rosterStatus;

    @Enumerated(EnumType.STRING)
    private OrgStatus orgStatus;

}
