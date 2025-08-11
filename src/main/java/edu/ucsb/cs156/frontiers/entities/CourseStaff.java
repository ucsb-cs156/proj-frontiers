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

  // Note that firstName and lastName are redundant with the information in the
  // user object; however the user object is not populated until the first time that user
  // logs into the system.  That is why we store these redundantly.
  private String firstName;
  private String lastName;
  private String email;

  private OrgStatus orgStatus;
  private Integer githubId;
  private String githubLogin;

  @ManyToOne
  @JoinColumn(name = "course_id")
  @ToString.Exclude
  private Course course;

  private String role;
}
