package edu.ucsb.cs156.frontiers.testconfig;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.services.CurrentUserServiceImpl;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class MockCurrentUserServiceImpl extends CurrentUserServiceImpl {

    public User getMockUser(SecurityContext securityContext, Authentication authentication) {
        Object principal = authentication.getPrincipal();

        String googleSub = "fakeUser";
        String email = "user@example.org";
        String pictureUrl = "https://example.org/fake.jpg";
        String fullName = "Fake User";
        String givenName = "Fake";
        String familyName = "User";
        boolean admin = false;

        org.springframework.security.core.userdetails.User user = null;


        if (principal instanceof org.springframework.security.core.userdetails.User) {
            user = (org.springframework.security.core.userdetails.User) principal;
            googleSub = "fake_" + user.getUsername();
            email = user.getUsername() + "@example.org";
            pictureUrl = "https://example.org/" + user.getUsername() + ".jpg";
            fullName = "Fake " + user.getUsername();
            givenName = "Fake";
            familyName = user.getUsername();
            admin = (user.getUsername().equals("admin"));
        } else if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;
            googleSub = oAuth2User.getAttribute("sub");
            email = oAuth2User.getAttribute("email");
            pictureUrl = oAuth2User.getAttribute("picture");
            fullName = oAuth2User.getAttribute("name");
            givenName = oAuth2User.getAttribute("givenname");
            familyName = oAuth2User.getAttribute("familyname");
            if (givenName !=null && givenName.equals("admin")) {
                admin = true;
            }

        }

        User u = User.builder()
                .googleSub(googleSub)
                .email(email)
                .pictureUrl(pictureUrl)
                .fullName(fullName)
                .givenName(givenName)
                .familyName(familyName)
                .admin(admin)
                .githubLogin("fake_login")
                .githubId(123456789)
                .id(1L)
                .build();

        return u;
    }

    @Override
    public User getUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        if (authentication != null) {
            return getMockUser(securityContext, authentication);
        }

        return null;
    }

}
