package edu.ucsb.cs156.frontiers.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.client.HttpSyncGraphQlClient;

public class GithubGraphQLClientConfigTests {
    @Test
    public void testGraphQlClientBean() {
        // This test is just to ensure that the graphQlClient bean can be created
        // without throwing any exceptions.
        // The actual functionality is tested in the service tests.
        GithubGraphQLClientConfig config = new GithubGraphQLClientConfig();
        HttpSyncGraphQlClient client = config.graphQlClient();
        assertNotNull(config);
        assertNotNull(client);
    }
}
