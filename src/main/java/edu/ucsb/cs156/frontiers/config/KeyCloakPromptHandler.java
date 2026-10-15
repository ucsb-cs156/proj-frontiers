package edu.ucsb.cs156.frontiers.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest.Builder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class KeyCloakPromptHandler implements Consumer<Builder> {

  @Override
  public void accept(Builder builder) {
    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    if (!Objects.equals(request.getRequestURI(), "/oauth2/authorization/keycloak")) {
      return;
    }
    if (Objects.equals(request.getParameter("idp"), "google")) {
      builder.additionalParameters(Map.of("kc_idp_hint", "google"));
    } else if (Objects.equals(request.getParameter("idp"), "microsoft")) {
      builder.additionalParameters(Map.of("kc_idp_hint", "microsoft"));
    } else {
      builder.additionalParameters(Map.of("kc_idp_hint", "google"));
    }
  }
}
