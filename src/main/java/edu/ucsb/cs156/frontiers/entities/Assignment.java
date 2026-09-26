package edu.ucsb.cs156.frontiers.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ucsb.cs156.frontiers.enums.AssignmentType;
import edu.ucsb.cs156.frontiers.enums.Permission;
import edu.ucsb.cs156.frontiers.enums.RepositoryCreationOption;
import edu.ucsb.cs156.frontiers.enums.Visibility;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * An assignment of a course: the settings that repositories are created with for it. Which fields
 * apply depends on {@link #asnType}:
 *
 * <ul>
 *   <li>INDIVIDUAL: {@link #createReposFor} is set (STUDENTS_ONLY, STAFF_ONLY or
 *       STUDENTS_AND_STAFF) and {@link #teamRegex} is null.
 *   <li>TEAM: {@link #teamRegex} is set (".*" for every team) and {@link #createReposFor} is null.
 * </ul>
 *
 * The type cannot be changed once the assignment exists.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "assignments")
public class Assignment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "course_id")
  @JsonIgnore
  @ToString.Exclude
  private Course course;

  /** Each repository created for the assignment begins with this prefix. */
  private String repoPrefix;

  @Enumerated(EnumType.STRING)
  private AssignmentType asnType;

  @Enumerated(EnumType.STRING)
  private Visibility visibility;

  @Enumerated(EnumType.STRING)
  private Permission permission;

  /** Whom repositories are created for; only for INDIVIDUAL assignments, null for TEAM ones. */
  @Enumerated(EnumType.STRING)
  private RepositoryCreationOption createReposFor;

  /**
   * Only teams whose names match this regular expression get a repository; only for TEAM
   * assignments, null for INDIVIDUAL ones.
   */
  private String teamRegex;

  /**
   * The id of the last job started to create this assignment's repositories, or null if none has
   * been started. It is not a foreign key: jobs can be deleted, and the jobs table belongs to the
   * lib-jobs library.
   */
  private Long lastJobId;

  /**
   * Whether the repositories created for this assignment must have signed commits: they get a
   * "Require Signed Commits" ruleset when the assignment's job runs, and if this is false the
   * ruleset is removed from them.
   */
  private boolean requireSignedCommit;
}
