package edu.ucsb.cs156.frontiers.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Tag(name = "Webhooks Controller")
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {


    private final CourseRepository courseRepository;
    private final RosterStudentRepository rosterStudentRepository;

    public WebhookController(CourseRepository courseRepository, RosterStudentRepository rosterStudentRepository) {
        this.courseRepository = courseRepository;
        this.rosterStudentRepository = rosterStudentRepository;
    }

    /**
    * Accepts webhooks from GitHub, currently to update the membership status of a RosterStudent.
    * @param jsonBody body of the webhook. The description of the currently used webhook is available in docs/webhooks.md
    *
    * @return either the word success so GitHub will not flag the webhook as a failure, or the updated RosterStudent
    */
    @PostMapping("/github")
    public ResponseEntity<String> createGitHubWebhook(@RequestBody JsonNode jsonBody) throws JsonProcessingException {

        if(jsonBody.has("action")){
            String action = jsonBody.get("action").asText();
            
            // Handle member_added and member_invited events
            if(action.equals("member_added") || action.equals("member_invited")){
                // Add null checks for each level of the JSON structure
                if (jsonBody.has("membership") && jsonBody.get("membership").has("user") && 
                    jsonBody.get("membership").get("user").has("login") && 
                    jsonBody.has("installation") && jsonBody.get("installation").has("id")) {
                    
                    String githubLogin = jsonBody.get("membership").get("user").get("login").asText();
                    String installationId = jsonBody.get("installation").get("id").asText();
                    Optional<Course> course = courseRepository.findByInstallationId(installationId);
                    
                    if(course.isPresent()){
                        Optional<RosterStudent> student = rosterStudentRepository.findByCourseAndGithubLogin(course.get(), githubLogin);
                        if(student.isPresent()){
                            RosterStudent updatedStudent = student.get();
                            
                            // Update status based on action
                            if(action.equals("member_added")) {
                                updatedStudent.setOrgStatus(OrgStatus.MEMBER);
                            } else if(action.equals("member_invited")) {
                                updatedStudent.setOrgStatus(OrgStatus.INVITED);
                            }
                            
                            rosterStudentRepository.save(updatedStudent);
                            return ResponseEntity.ok(updatedStudent.toString());
                        }
                    }
                }
            }
        }
        
        return ResponseEntity.ok().body("success");
    }
}
