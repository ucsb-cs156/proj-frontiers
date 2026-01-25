package edu.ucsb.cs156.frontiers.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;

@Configuration
public class GithubGraphQLClientConfig {
  @Autowired private RestClient.Builder restClientBuilder;

  @Bean
  public HttpSyncGraphQlClient graphQlClient() {
    RestClient restClient = restClientBuilder.baseUrl("https://api.github.com/graphql").build();
    HttpSyncGraphQlClient graphQlClient = HttpSyncGraphQlClient.builder(restClient).build();
    return graphQlClient;
  }
}
