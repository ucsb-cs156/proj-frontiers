package edu.ucsb.cs156.frontiers.models;

import com.opencsv.bean.CsvBindByName;
import edu.ucsb.cs156.frontiers.entities.CourseStaff;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** CSV export DTO for course staff downloads. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseStaffDTO {

  @CsvBindByName(column = "id")
  private Long id;

  @CsvBindByName(column = "courseId")
  private Long courseId;

  @CsvBindByName(column = "userId")
  private Long userId;

  @CsvBindByName(column = "firstName")
  private String firstName;

  @CsvBindByName(column = "lastName")
  private String lastName;

  @CsvBindByName(column = "email")
  private String email;

  @CsvBindByName(column = "orgStatus")
  private OrgStatus orgStatus;

  @CsvBindByName(column = "githubId")
  private Integer githubId;

  @CsvBindByName(column = "githubLogin")
  private String githubLogin;

  @CsvBindByName(column = "role")
  private String role;

  public CourseStaffDTO(CourseStaff courseStaff) {
    this(
        courseStaff.getId(),
        courseStaff.getCourse().getId(),
        courseStaff.getUser() != null ? courseStaff.getUser().getId() : 0L,
        courseStaff.getFirstName(),
        courseStaff.getLastName(),
        courseStaff.getEmail(),
        courseStaff.getOrgStatus(),
        courseStaff.getGithubId(),
        courseStaff.getGithubLogin(),
        courseStaff.getRole());
  }
}
