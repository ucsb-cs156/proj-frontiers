package edu.ucsb.cs156.example.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;

import edu.ucsb.cs156.example.services.wiremock.WiremockService;

import java.net.ConnectException;

/**
 * The FrontendProxyController is used to proxy requests to the frontend of the application.
 * 
 * This is only used in development where we have a separate frontend server process
 * listening on port 3000.
 * 
 * For production, see the FrontendController.
 * 
 * @see edu.ucsb.cs156.example.controllers.FrontendController
 */

@Profile("development")
@RestController
public class FrontendProxyController {

  @Autowired
  WiremockService wiremockService;
  
  /**
   * This method proxies requests to the frontend server.  It is only used in development.
   * The regular expression is used to exclude the paths that should NOT be proxied to the
   * frontend server, such as the endpoints for the api, oauth2, and swagger-ui.
   * 
   * @param proxy the proxy exchange, injected by Spring automatically
   * @return response entity with the response from the frontend server, or a response entity with instructions in case the frontend server cannot be reached.
   */

  @GetMapping({"/", "/{path:^(?!api|oauth2|swagger-ui|h2-console).*}/**"})
  public ResponseEntity<?> proxy(ProxyExchange<byte []> proxy) {
    String path = proxy.path("/");
    try {
      return proxy.uri("http://localhost:3000/" + path).get();
    } catch (ResourceAccessException e) {
      if (e.getCause() instanceof ConnectException) {
        String instructions = """
                <p>Failed to connect to the frontend server...</p>
                <p>On Dokku, be sure that <code>PRODUCTION</code> is defined.</p>
                <p>On localhost, open a second terminal window, cd into <code>frontend</code> and type: <code>nvm use 20.17.0; npm ci; npm start</code></p>
                <p>Or, you may click to access: </p>
                <ul>
                  <li><a href='/swagger-ui/index.html'>/swagger-ui/index.html</a></li>
                  <li><a href='/h2-console'>/h2-console</a></li>
                </ul>""";

        return ResponseEntity.ok(instructions);
      }
      throw e;
    }
  }
}
