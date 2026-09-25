package edu.ucsb.cs156.frontiers.config;

import edu.ucsb.cs156.frontiers.services.ApiKeyService;
import java.security.SecureRandom;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiKeyConfig {

  @Bean
  public ApiKeyFilter apiKeyFilter(ApiKeyService apiKeyService) {
    return new ApiKeyFilter(apiKeyService);
  }

  @Bean
  public FilterRegistrationBean<ApiKeyFilter> disableFilterRegistration(ApiKeyFilter filter) {
    FilterRegistrationBean<ApiKeyFilter> registrationBean = new FilterRegistrationBean<>(filter);
    registrationBean.setEnabled(false);
    return registrationBean;
  }

  @Bean
  public SecureRandom secureRandomProvider() {
    return new SecureRandom();
  }
}
