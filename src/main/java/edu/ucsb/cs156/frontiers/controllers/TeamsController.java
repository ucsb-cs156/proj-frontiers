package edu.ucsb.cs156.frontiers.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamMemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Teams")
@RequestMapping("/api/teams")
@RestController
@Slf4j
public class TeamsController extends ApiController {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RosterStudentRepository rosterStudentRepository;

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
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

        Team team = Team.builder()
                .name(name)
                .course(course)
                .build();
        Team savedTeam = teamRepository.save(team);

        return savedTeam;
    }

    /**
     * This method returns a list of all teams for a course
     * 
     * @return a list of all teams for a course
     */
    @Operation(summary = "List all teams")
    @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
    @GetMapping("/all")
    public Iterable<Team> allTeams(Long courseId) {
        Iterable<Team> teams = teamRepository.findByCourseId(courseId);
        return teams;
    }

    /**
     * This method returns a single team by its id
     * 
     * @param id the id of the team
     * @return the team
     */
    @Operation(summary = "Get a single team")
    @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
    @GetMapping("")
    public Team getTeamById(@Parameter(name = "id") @RequestParam Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Team.class, id));
        return team;
    }

    /**
     * This method deletes a team by its id
     * 
     * @param id the id of the team to delete
     * @return a message indicating the team was deleted
     */
    @Operation(summary = "Delete a team")
    @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
    @DeleteMapping("")
    public Object deleteTeam(@Parameter(name = "id") @RequestParam Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Team.class, id));
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
    @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
    @PostMapping("/addMember")
    public TeamMember addTeamMember(
            @Parameter(name = "teamId") @RequestParam Long teamId,
            @Parameter(name = "rosterStudentId") @RequestParam Long rosterStudentId) {
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException(Team.class, teamId));
        
        RosterStudent rosterStudent = rosterStudentRepository.findById(rosterStudentId)
                .orElseThrow(() -> new EntityNotFoundException(RosterStudent.class, rosterStudentId));

        TeamMember teamMember = TeamMember.builder()
                .team(team)
                .rosterStudent(rosterStudent)
                .build();
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
    @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
    @DeleteMapping("/removeMember")
    public Object removeTeamMember(@Parameter(name = "teamMemberId") @RequestParam Long teamMemberId) {
        TeamMember teamMember = teamMemberRepository.findById(teamMemberId)
                .orElseThrow(() -> new EntityNotFoundException(TeamMember.class, teamMemberId));
        teamMemberRepository.delete(teamMember);
        return genericMessage("Team member with id %s deleted".formatted(teamMemberId));
    }

    public enum TeamSourceType {
        SIMPLE,
        UNKNOWN
    }

    public static final String SIMPLE_HEADERS = "team,email";

    public static TeamSourceType getRosterSourceType(String [] headers) {

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
        // If no known type matches, return UNKNOWN
        return TeamSourceType.UNKNOWN;
    }

}