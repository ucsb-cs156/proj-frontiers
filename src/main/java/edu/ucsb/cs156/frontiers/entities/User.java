package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.*;
import lombok.*;

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
}
