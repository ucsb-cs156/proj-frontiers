package edu.ucsb.cs156.frontiers.testconfig;

import edu.ucsb.cs156.frontiers.config.SecurityConfig;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import edu.ucsb.cs156.frontiers.services.GithubSignInService;
import edu.ucsb.cs156.frontiers.services.GoogleSignInService;
import edu.ucsb.cs156.frontiers.services.GrantedAuthoritiesService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(SecurityConfig.class)
public class IntegrationConfig {

  @Bean
  public CurrentUserService currentUserService() {
    return new MockCurrentUserServiceImpl();
  }

  @Bean
  public GrantedAuthoritiesService grantedAuthoritiesService() {
    return new GrantedAuthoritiesService();
  }

  @Bean
  public GithubSignInService githubSignInService() {
    return new MockGithubSignInService();
  }

  @Bean
  public GoogleSignInService googleSignInService() {
    return new MockGoogleSignInService();
  }
}
