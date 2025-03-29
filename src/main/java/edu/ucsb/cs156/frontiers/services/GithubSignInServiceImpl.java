package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.errors.NotAuthenticatedWithGoogleException;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;


@Service
public class GithubSignInServiceImpl extends DefaultOAuth2UserService implements GithubSignInService {

    private final UserRepository userRepository;

    private final CurrentUserService currentUserService;
    private final RosterStudentRepository rosterStudentRepository;
    private final UpdateUserService updateUserService;

    public GithubSignInServiceImpl(@Autowired UserRepository userRepository, @Autowired CurrentUserService currentUserService, @Autowired RosterStudentRepository rosterStudentRepository, @Autowired UpdateUserService updateUserService) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.rosterStudentRepository = rosterStudentRepository;
        this.updateUserService = updateUserService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);
        if (request.getClientRegistration().getRegistrationId().equals("github")){
            return attachGithubSignin(oAuth2User);
        }
        else{
            return oAuth2User;
        }
    }

    private OAuth2User attachGithubSignin(OAuth2User oAuth2User) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        OidcUser currentUser;
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (authentication != null) {
            currentUser = (OidcUser) authentication.getPrincipal();
            User currentLocalUser = currentUserService.getCurrentUser().getUser();
            if (currentLocalUser != null) {
                currentLocalUser.setGithubId((Integer) oAuth2User.getAttributes().get("id"));
                currentLocalUser.setGithubLogin((String) oAuth2User.getAttributes().get("login"));
                updateUserService.attachRosterStudents(currentLocalUser);
                userRepository.save(currentLocalUser);
            }
            authorities.add(new SimpleGrantedAuthority("ROLE_GITHUB"));
            authorities.addAll(currentUser.getAuthorities());
            return new DefaultOidcUser(authorities, currentUser.getIdToken(), currentUser.getUserInfo());
        } else {
            throw new NotAuthenticatedWithGoogleException("You must login first to link your GitHub account");
        }

    }
}
