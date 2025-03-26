package edu.ucsb.cs156.frontiers.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

  
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The CSRF controller is used to get a CSRF token. 
 * This is only enabled in the development profile, 
 * and is used to test APIs with Postman or swagger.ui/
 * 
 * For more information on CSRF, do a web search on "Cross-Site Request Forgery".
 */

@Profile("development")
@Tag(name = "CSRF (enabled only in development; can be used with Postman to test APIs)")
@RestController
public class CSRFController {

  /**
   * This method returns a CSRF token.
   * @param token the CSRF token, injected by Spring automatically
   * @return the CSRF token
   */
  @Operation(summary= "Get a CSRF Token")
  @GetMapping("/csrf")
  public CsrfToken csrf(CsrfToken token) {
    return token;
  }
}
