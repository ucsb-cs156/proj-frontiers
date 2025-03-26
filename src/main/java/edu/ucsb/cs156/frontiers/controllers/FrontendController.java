package edu.ucsb.cs156.frontiers.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;

/**
 * The FrontendController is used to serve the frontend of the application.
 * 
 * This is only enabled in the production profile, and is used to serve the frontend of the application.
 * For development, see the FrontendProxyController.
 * 
 * @see edu.ucsb.cs156.frontiers.controllers.FrontendProxyController
 */

@Profile("!development")
@Controller
public class FrontendController {

  @Autowired
  WiremockService wiremockService;
  
  /**
   * Serve home page of application
   * @return the home page (via index.html)
   */

  @GetMapping("/**/{path:[^\\.]*}")
  public String index() {
    return "forward:/index.html";
  }

  /**
   * When not in development, the CSRF endpoint is not used, so return 404
   * @return response entity with 404 return code (not found)
   */
  
  @GetMapping("/csrf")
  public ResponseEntity<String> csrf() {
    return ResponseEntity.notFound().build();
  }

}
