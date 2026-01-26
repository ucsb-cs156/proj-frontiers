package edu.ucsb.cs156.frontiers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;

@Configuration
public class GithubGraphQLClientConfig {

  @Bean
  public HttpSyncGraphQlClient graphQlClient() {
    RestClient restClient = RestClient.create("https://api.github.com/graphql");
    HttpSyncGraphQlClient graphQlClient = HttpSyncGraphQlClient.builder(restClient).build();
    return graphQlClient;
  }
}
