package edu.ucsb.cs156.frontiers.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.services.JwtService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Builder
public class DeleteRepoJob implements JobContextConsumer {

  Course course;
  String prefix;
  JwtService jwtService;
  RestTemplate restTemplate;
  ObjectMapper mapper;

  @Override
  public Course getCourse() {
    return course;
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    String orgName = course.getOrgName();
    String token = jwtService.getInstallationToken(course);

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    List<String> matchedRepos = new ArrayList<>();

    // 1. Fetch all repositories for the organization (Handling Pagination)
    String reposUrl = "https://api.github.com/orgs/" + orgName + "/repos?per_page=100";
    boolean hasNext = true;

    while (hasNext) {
      ResponseEntity<String> response =
          restTemplate.exchange(reposUrl, HttpMethod.GET, entity, String.class);
      JsonNode reposNode = mapper.readTree(response.getBody());

      for (JsonNode repoNode : reposNode) {
        String repoName = repoNode.get("name").asText();
        if (repoName.startsWith(prefix)) {
          matchedRepos.add(repoName);
        }
      }

      // Check for pagination "Link" header to get the next page
      List<String> linkHeaders = response.getHeaders().get("Link");
      hasNext = false;
      if (linkHeaders != null && !linkHeaders.isEmpty()) {
        String linkHeader = linkHeaders.get(0);
        if (linkHeader.contains("rel=\"next\"")) {
          String[] parts = linkHeader.split(",");
          for (String part : parts) {
            if (part.contains("rel=\"next\"")) {
              reposUrl = part.substring(part.indexOf('<') + 1, part.indexOf('>'));
              hasNext = true;
              break;
            }
          }
        }
      }
    }

    ctx.log(String.format("%d repos found with prefix %s", matchedRepos.size(), prefix));

    int reposDeleted = 0;
    int reposRetained = 0;
    int errors = 0;

    // 2. Iterate through matched repos to check for commits and delete
    for (String repoName : matchedRepos) {
      try {
        // Sleep delay to prevent hitting GitHub API rate limits
        Thread.sleep(1000);

        String commitsUrl = "https://api.github.com/repos/" + orgName + "/" + repoName + "/commits";
        boolean hasCommits = true;

        try {
          ResponseEntity<String> commitsResponse =
              restTemplate.exchange(commitsUrl, HttpMethod.GET, entity, String.class);
          JsonNode commitsNode = mapper.readTree(commitsResponse.getBody());
          hasCommits = commitsNode.isArray() && !commitsNode.isEmpty();
        } catch (HttpClientErrorException e) {
          // GitHub API returns 409 Conflict if a repository is completely empty (no commits)
          if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
            hasCommits = false;
          } else {
            throw e; // Rethrow actual errors to be caught below
          }
        }

        if (hasCommits) {
          reposRetained++;
          ctx.log(String.format("Repo %s not delete; commits exist.", repoName));
        } else {
          String deleteUrl = "https://api.github.com/repos/" + orgName + "/" + repoName;
          restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, String.class);
          reposDeleted++;
        }
      } catch (Exception e) {
        errors++;
        ctx.log(String.format("Error processing repo %s: %s", repoName, e.getMessage()));
      }
    }

    // 3. Final Logging
    ctx.log(String.format("%d repos deleted", reposDeleted));
    ctx.log(String.format("%d repos retained", reposRetained));
    ctx.log(String.format("%d errors", errors));
  }
}
