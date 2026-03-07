package edu.ucsb.cs156.frontiers.config;

import edu.ucsb.cs156.frontiers.interceptors.RateLimitInterceptor;
import edu.ucsb.cs156.frontiers.interceptors.RoleUpdateInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Autowired private RoleUpdateInterceptor roleUpdateInterceptor;
  @Autowired private RateLimitInterceptor rateLimitInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(roleUpdateInterceptor);
  }

  @Bean
  public RestTemplateCustomizer customize() {
    return restTemplate -> restTemplate.getInterceptors().add(rateLimitInterceptor);
  }

  @Bean
  public RestClientCustomizer customizeRestClient() {
    return customizer -> customizer.requestInterceptor(rateLimitInterceptor);
  }
}
