package edu.ucsb.cs156.frontiers.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    

    @PostMapping("/github")
    public ResponseEntity<String> createGitHubWebhook(@RequestBody JsonNode jsonBody) throws JsonProcessingException {
        if(jsonBody.get("action").toString().equals("member_added")){
            String githubLogin = jsonBody.get("membership").get("user").get("login").toString();
        }
        return  ResponseEntity.ok().body("success");
    }
}
