package edu.ucsb.cs156.frontiers.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {


    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final RosterStudentRepository rosterStudentRepository;

    public WebhookController(CourseRepository courseRepository, UserRepository userRepository, RosterStudentRepository rosterStudentRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.rosterStudentRepository = rosterStudentRepository;
    }

    @PostMapping("/github")
    public ResponseEntity<String> createGitHubWebhook(@RequestBody JsonNode jsonBody) throws JsonProcessingException {

        if(jsonBody.has("action")){
            if(jsonBody.get("action").asText().equals("member_added")){
                String githubLogin = jsonBody.get("membership").get("user").get("login").asText();
                String installationId = jsonBody.get("installation").get("id").asText();
                Optional<Course> course = courseRepository.findByInstallationId(installationId);
                Optional<User> user = userRepository.findByGithubLogin(githubLogin);
                if(course.isPresent() && user.isPresent()){
                    Optional<RosterStudent> student = rosterStudentRepository.findByCourseAndUser(course.get(), user.get());
                    if(student.isPresent()){
                        RosterStudent updatedStudent = student.get();
                        updatedStudent.setOrgStatus(OrgStatus.MEMBER);
                        rosterStudentRepository.save(updatedStudent);
                        return ResponseEntity.ok(updatedStudent.toString());
                    }
                }
            }
        }
        return  ResponseEntity.ok().body("success");
    }
}
