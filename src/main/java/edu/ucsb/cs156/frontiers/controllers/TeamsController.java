package edu.ucsb.cs156.frontiers.controllers;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.jobs.DeleteTeamMemberFromGithubJob;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.services.GithubTeamService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Teams")
@RequestMapping("/api/teams")
@RestController
@Slf4j
public class TeamsController extends ApiController {

  @Autowired private TeamRepository teamRepository;

  @Autowired private TeamMemberRepository teamMemberRepository;

  @Autowired private CourseRepository courseRepository;

  @Autowired private RosterStudentRepository rosterStudentRepository;

  @Autowired private JobService jobService;

  @Autowired private GithubTeamService githubTeamService;

  public record TeamMemberResult(
      TeamMember teamMember, TeamMemberStatus status, String rejectedEmail) {
    public TeamMemberResult(TeamMember teamMember, TeamMemberStatus status) {
      this(teamMember, status, null);
    }

    public TeamMemberResult(String rejectedEmail) {
      this(null, TeamMemberStatus.MISSING, rejectedEmail);
    }
  }

  public record TeamCreationResponse(
      TeamSourceType typeMatched, Integer created, Integer existing, List<String> rejected) {}

  public record TeamMemberMapping(
      Long teamId,
      String teamName,
      Long rosterStudentId,
      String email,
      String firstName,
      String lastName,
      String githubLogin) {
    public static TeamMemberMapping from(TeamMember member) {
      return new TeamMemberMapping(
          member.getTeam().getId(),
          member.getTeam().getName(),
          member.getRosterStudent().getId(),
          member.getRosterStudent().getEmail(),
          member.getRosterStudent().getFirstName(),
          member.getRosterStudent().getLastName(),
          member.getRosterStudent().getGithubLogin());
    }
  }

  /**
   * This method creates a new Team.
   *
   * @param name the name of the team
   * @param courseId the ID of the course this team belongs to
   * @return the created team
   */
  @Operation(summary = "Create a new team")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/post")
  public Team postTeam(
      @Parameter(name = "name") @RequestParam String name,
      @Parameter(name = "courseId") @RequestParam Long courseId) {

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

    Team team = Team.builder().name(name).course(course).build();

    if (teamRepository.findByCourseIdAndName(course.getId(), name).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Team with name %s already exists".formatted(name));
    } else {
      team = teamRepository.save(team);
    }

    return team;
  }

  /**
   * Upload teams in CSV format (team, email) It is important to keep the code in this method
   * consistent with the code for adding a single roster student
   *
   * @param courseId course the teams are for
   * @param file csv file with roster student emails and team assignments
   * @return Count of students added to teams, already existing, and rejected students
   */
  @Operation(summary = "Upload team assignments; CSV in format team,email")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping(
      value = "/upload/csv",
      consumes = {"multipart/form-data"})
  public ResponseEntity<TeamCreationResponse> uploadTeamsCsv(
      @Parameter(name = "courseId") @RequestParam Long courseId,
      @Parameter(name = "file") @RequestParam("file") MultipartFile file)
      throws IOException, CsvException {

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId.toString()));

    int counts[] = {0, 0};

    List<String> failed = new ArrayList<>();

    try (InputStream inputStream = new BufferedInputStream(file.getInputStream());
        InputStreamReader reader = new InputStreamReader(inputStream);
        CSVReader csvReader = new CSVReader(reader); ) {

      String[] headers = csvReader.readNext();
      TeamSourceType sourceType = getRosterSourceType(headers);
      List<String[]> myEntries = csvReader.readAll();
      for (String[] row : myEntries) {
        TeamMemberResult rowResult = fromCSVRow(row, sourceType, course);
        if (rowResult.status == TeamMemberStatus.MISSING) {
          failed.add(rowResult.rejectedEmail);
        } else {
          counts[rowResult.status.ordinal()]++;
        }
      }
      TeamCreationResponse response =
          new TeamCreationResponse(
              sourceType,
              counts[TeamMemberStatus.CREATED.ordinal()],
              counts[TeamMemberStatus.EXISTS.ordinal()],
              failed);

      if (!failed.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
      } else {
        return ResponseEntity.ok(response);
      }
    }
  }

  /**
   * This method returns a list of all teams for a course
   *
   * @return a list of all teams for a course
   */
  @Operation(summary = "List all teams")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/all")
  public Iterable<Team> allTeams(@RequestParam Long courseId) {
    Iterable<Team> teams = teamRepository.findByCourseIdOrderByNameAsc(courseId);
    return teams;
  }

  /**
   * Retrieves a list of mappings between roster students and teams for a given course. Each mapping
   * represents a relationship between a team and its members.
   *
   * @param courseId the unique identifier of the course for which team mappings are retrieved
   * @return an iterable collection of {@code TeamMemberMapping} objects representing the mappings
   */
  @Operation(summary = "List the mapping of Roster Students to Teams")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("/mapping")
  public Iterable<TeamMemberMapping> teamMemberMapping(@RequestParam Long courseId) {
    List<Team> teams =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId))
            .getTeams();
    List<TeamMemberMapping> mappings = new ArrayList<>();
    for (Team team : teams) {
      for (TeamMember member : team.getTeamMembers()) {
        mappings.add(TeamMemberMapping.from(member));
      }
    }
    return mappings;
  }

  /**
   * This method returns a single team by its id
   *
   * @param id the id of the team
   * @return the team
   */
  @Operation(summary = "Get a single team")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @GetMapping("")
  public Team getTeamById(
      @Parameter(name = "id") @RequestParam Long id, @RequestParam Long courseId) {
    Team team =
        teamRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(Team.class, id));
    return team;
  }

  /**
   * This method deletes a team by its id
   *
   * @param id the id of the team to delete
   * @return a message indicating the team was deleted
   */
  @Operation(summary = "Delete a team")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @DeleteMapping("")
  @Transactional
  public Object deleteTeam(
      @Parameter(name = "id") @RequestParam Long id, @RequestParam Long courseId) {
    Team team =
        teamRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(Team.class, id));

    // Handle team members that reference this team
    if (!team.getTeamMembers().isEmpty()) {
      team.getTeamMembers()
          .forEach(
              teamMember -> {
                // Remove from roster student's team members list
                teamMember.getRosterStudent().getTeamMembers().remove(teamMember);
                teamMember.setRosterStudent(null);
              });
    }

    // Disconnect from course
    team.getCourse().getTeams().remove(team);
    team.setCourse(null);

    teamRepository.delete(team);
    return genericMessage("Team with id %s deleted".formatted(id));
  }

  /**
   * This method adds a roster student as a team member
   *
   * @param teamId the ID of the team
   * @param rosterStudentId the ID of the roster student to add
   * @return the created team member
   */
  @Operation(summary = "Add a roster student to a team")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @PostMapping("/addMember")
  public TeamMember addTeamMember(
      @Parameter(name = "teamId") @RequestParam Long teamId,
      @Parameter(name = "rosterStudentId") @RequestParam Long rosterStudentId,
      @Parameter(name = "courseId") @RequestParam Long courseId) {

    Team team =
        teamRepository
            .findById(teamId)
            .orElseThrow(() -> new EntityNotFoundException(Team.class, teamId));

    RosterStudent rosterStudent =
        rosterStudentRepository
            .findById(rosterStudentId)
            .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, rosterStudentId));

    if (!team.getCourse().getId().equals(courseId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Team is not from course %d".formatted(courseId));
    }
    if (!rosterStudent.getCourse().getId().equals(courseId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Roster student is not from course %d".formatted(courseId));
    }

    if (teamMemberRepository.findByTeamAndRosterStudent(team, rosterStudent).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Team member already exists for team %s and roster student %s"
              .formatted(team.getName(), rosterStudent.getEmail()));
    }
    TeamMember teamMember = TeamMember.builder().team(team).rosterStudent(rosterStudent).build();
    TeamMember savedTeamMember = teamMemberRepository.save(teamMember);

    return savedTeamMember;
  }

  /**
   * This method removes a team member
   *
   * @param teamMemberId the ID of the team member to remove
   * @return a message indicating the team member was removed
   */
  @Operation(summary = "Remove a team member")
  @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
  @DeleteMapping("/removeMember")
  @Transactional
  public Object removeTeamMember(
      @Parameter(name = "teamMemberId") @RequestParam Long teamMemberId,
      @Parameter(name = "courseId") @RequestParam Long courseId) {
    TeamMember teamMember =
        teamMemberRepository
            .findById(teamMemberId)
            .orElseThrow(() -> new EntityNotFoundException(TeamMember.class, teamMemberId));
    Team team = teamMember.getTeam();
    RosterStudent rosterStudent = teamMember.getRosterStudent();

    DeleteTeamMemberFromGithubJob job =
        DeleteTeamMemberFromGithubJob.builder()
            .teamMemberId(teamMemberId)
            .teamId(team.getId())
            .teamMemberRepository(teamMemberRepository)
            .githubTeamService(githubTeamService)
            .build();
    jobService.runAsJob(job);

    team.getTeamMembers().remove(teamMember);
    rosterStudent.getTeamMembers().remove(teamMember);
    teamMemberRepository.delete(teamMember);
    teamRepository.save(team);
    rosterStudentRepository.save(rosterStudent);

    return genericMessage("Team member with id %s deleted".formatted(teamMemberId));
  }

  public enum TeamSourceType {
    SIMPLE,
    UNKNOWN
  }

  public enum TeamMemberStatus {
    CREATED,
    EXISTS,
    MISSING
  }

  public static final String SIMPLE_HEADERS = "team,email";

  public TeamSourceType getRosterSourceType(String[] headers) {

    Map<TeamSourceType, String[]> sourceTypeToHeaders = new HashMap<>();

    sourceTypeToHeaders.put(TeamSourceType.SIMPLE, SIMPLE_HEADERS.split(","));

    for (Map.Entry<TeamSourceType, String[]> entry : sourceTypeToHeaders.entrySet()) {
      TeamSourceType type = entry.getKey();
      String[] expectedHeaders = entry.getValue();
      if (headers.length >= expectedHeaders.length) {
        boolean matches = true;
        for (int i = 0; i < expectedHeaders.length; i++) {
          if (!expectedHeaders[i].equalsIgnoreCase(headers[i])) {
            matches = false;
            break;
          }
        }
        if (matches) {
          return type;
        }
      }
    }
    // If no known type matches, throw
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown Roster Source Type");
  }

  public TeamMemberResult fromCSVRow(String[] row, TeamSourceType sourceType, Course course) {
    // No if statements because this is the only possible value to enter here at the moment. Replace
    // with if when more
    // Formats are added.
    return teamMemberFromSimpleCsv(row, course);
  }

  public TeamMemberResult teamMemberFromSimpleCsv(String[] row, Course course) {
    Optional<RosterStudent> student =
        rosterStudentRepository.findByCourseIdAndEmail(course.getId(), row[1]);
    Optional<Team> team = teamRepository.findByCourseIdAndName(course.getId(), row[0]);
    if (student.isPresent() && team.isPresent()) {
      Optional<TeamMember> teamMember =
          teamMemberRepository.findByTeamAndRosterStudent(team.get(), student.get());
      if (teamMember.isPresent()) {
        return new TeamMemberResult(teamMember.get(), TeamMemberStatus.EXISTS);
      } else {
        TeamMember teamMemberToSave =
            TeamMember.builder().team(team.get()).rosterStudent(student.get()).build();
        TeamMember savedTeamMember = teamMemberRepository.save(teamMemberToSave);
        return new TeamMemberResult(savedTeamMember, TeamMemberStatus.CREATED);
      }
    } else if (student.isPresent()) {
      Team teamToSave = Team.builder().name(row[0]).course(course).build();
      teamRepository.save(teamToSave);
      TeamMember saveTeamMember =
          TeamMember.builder().team(teamToSave).rosterStudent(student.get()).build();
      teamMemberRepository.save(saveTeamMember);
      return new TeamMemberResult(saveTeamMember, TeamMemberStatus.CREATED);
    } else {
      return new TeamMemberResult(row[1]);
    }
  }
}
