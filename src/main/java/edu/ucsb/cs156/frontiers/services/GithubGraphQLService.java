package edu.ucsb.cs156.frontiers.services;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient.Builder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GithubGraphQLService {

    private final HttpSyncGraphQlClient graphQlClient;

    private final JwtService jwtService;

    public GithubGraphQLService(
            HttpSyncGraphQlClient graphQlClient,
            JwtService jwtService) {
        this.jwtService = jwtService;
        this.graphQlClient = graphQlClient;
    }

    /**
     * Retrieves the name of the default branch for a given GitHub repository.
     *
     * @param owner The owner (username or organization) of the repository.
     * @param repo  The name of the repository.
     * @return A Mono emitting the default branch name, or an empty Mono if not
     *         found.
     */
    public String getDefaultBranchName(Course course, String owner, String repo) throws JsonProcessingException,
            NoSuchAlgorithmException, InvalidKeySpecException, NoLinkedOrganizationException {
        log.info("getDefaultBranchName called with course.getId(): {} owner: {}, repo: {}", course.getId(), owner,
                repo);
        String githubToken = jwtService.getInstallationToken(course);

        log.info("githubToken: {}", githubToken);

        String query = """
                query getDefaultBranch($owner: String!, $repo: String!) {
                  repository(owner: $owner, name: $repo) {
                    defaultBranchRef {
                      name
                    }
                  }
                }
                """;

        return graphQlClient.mutate()
                .header("Authorization", "Bearer " + githubToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .document(query)
                .variable("owner", owner)
                .variable("repo", repo)
                .retrieveSync("repository.defaultBranchRef.name")
                .toEntity(String.class);
    }
}
