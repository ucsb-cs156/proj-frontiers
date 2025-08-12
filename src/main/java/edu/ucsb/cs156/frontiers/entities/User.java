package edu.ucsb.cs156.frontiers.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/** This is a JPA entity that represents a user. */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Entity(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  private String email;
  private String googleSub;
  private String pictureUrl;
  private String fullName;
  private String givenName;
  private String familyName;
  private Integer githubId;
  private String githubLogin;
  private String studentId;

  @JsonIgnore
  @OneToMany(mappedBy = "user")
  @Fetch(FetchMode.JOIN)
  @ToString.Exclude
  private List<RosterStudent> linkedStudents;

  @OneToMany(mappedBy = "user")
  @Fetch(FetchMode.JOIN)
  @ToString.Exclude
  @JsonIgnore
  private List<CourseStaff> roles;
}
