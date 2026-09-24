package edu.ucsb.cs156.frontiers.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ucsb.cs156.frontiers.enums.School;
import jakarta.persistence.*;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Course {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String installationId;

  private String orgName;

  private String instructorEmail;

  private String courseName;

  private String term;

  @Enumerated(EnumType.STRING)
  private School school;

  @JsonIgnore private String canvasApiToken;

  private String canvasCourseId;

  @JsonIgnore @ToString.Exclude private String slackBotToken;

  private String slackTeamId;

  private String slackTeamName;

  private String slackTeamUrl;

  private boolean hideBasePermissionWarning;

  /**
   * Extra lines placed at the very start of dokku_users_list.csv, e.g. for people who need dokku
   * access but are not part of the course. One username,dokku-nn entry per line.
   */
  @Column(length = 1024)
  private String dokkuUsersListHeader;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
  @Fetch(FetchMode.JOIN)
  @JsonIgnore
  @ToString.Exclude
  private List<CourseStaff> courseStaff;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
  @Fetch(FetchMode.JOIN)
  @JsonIgnore
  @ToString.Exclude
  private List<RosterStudent> rosterStudents;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
  @Fetch(FetchMode.JOIN)
  @JsonIgnore
  @ToString.Exclude
  private List<Team> teams;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
  @Fetch(FetchMode.JOIN)
  @JsonIgnore
  @ToString.Exclude
  private List<Section> sections;
}
