package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * This is a JPA entity that represents a user.
 */

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
  private boolean admin;
  private boolean professor;
  private int githubId;
  private String githubLogin;
  private String studentId;

  @JsonIgnore
  @OneToMany(mappedBy = "user")
  @Fetch(FetchMode.JOIN)
  private List<RosterStudent> linkedStudents;

  @OneToMany(mappedBy = "user")
  @Fetch(FetchMode.JOIN)
  private List<CourseStaff> roles;
}
