package edu.ucsb.cs156.frontiers.entities;


import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class CourseStaff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    private String email; 

    @Enumerated(EnumType.STRING)
    private OrgStatus orgStatus; 

    @ManyToOne
    @JoinColumn(name = "course_id")
    @ToString.Exclude
    private Course course;

    private String role;
}
