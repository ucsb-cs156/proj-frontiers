package edu.ucsb.cs156.frontiers.testconfig;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.stereotype.Service;

import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.services.CurrentUserServiceImpl;

@Service("testingUser")
public class MockCurrentUserServiceImpl extends CurrentUserServiceImpl {

  public User getMockUser(SecurityContext securityContext, Authentication authentication) {
    Object principal = authentication.getPrincipal();

    String googleSub = "fakeUser";
    String email = "user@example.org";
    String pictureUrl = "https://example.org/fake.jpg";
    String fullName = "Fake User";
    String givenName = "Fake";
    boolean admin=false;

    org.springframework.security.core.userdetails.User user = null;


    if (principal instanceof org.springframework.security.core.userdetails.User) {
      user = (org.springframework.security.core.userdetails.User) principal;
      googleSub = "fake_" + user.getUsername();
      email = user.getUsername() + "@example.org";
      pictureUrl = "https://example.org/" +  user.getUsername() + ".jpg";
      fullName = "Fake " + user.getUsername();
      givenName = "Fake";
      admin= (user.getUsername().equals("admin"));
    }

    User u = User.builder()
    .googleSub(googleSub)
    .email(email)
    .pictureUrl(pictureUrl)
    .fullName(fullName)
    .givenName(givenName)
    .admin(admin)
    .id(1L)
    .build();
    
    return u;
  }

  public User getUser() {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    Authentication authentication = securityContext.getAuthentication();

    if (!(authentication instanceof OidcIdToken)) {
      return getMockUser(securityContext, authentication);
    }

    return null;
  }

}
