package edu.ucsb.cs156.frontiers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpGraphQlClient;

@Configuration
public class GraphQLClientConfig {

    @Bean
    public HttpGraphQlClient.Builder<?> graphQlClientBuilder() {
        return HttpGraphQlClient.builder();
    }
}