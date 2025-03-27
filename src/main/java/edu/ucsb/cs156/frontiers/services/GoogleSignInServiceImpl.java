package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.User;
import edu.ucsb.cs156.frontiers.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoogleSignInServiceImpl extends OidcUserService implements GoogleSignInService {

    private final UserRepository userRepository;

    @Value("${app.admin.emails}")
    private final List<String> adminEmails = new ArrayList<>();

    @Autowired
    public GoogleSignInServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException{
        OidcUser oidcUser = super.loadUser(userRequest);
        return managePrimarySignIn(oidcUser);
    }

    private OidcUser managePrimarySignIn(OidcUser oidcUser){
        Optional<User> currentUser = userRepository.findByGoogleSub(oidcUser.getSubject());
        Set<GrantedAuthority> authorities = new HashSet<>();
        boolean changed = false;
        if (currentUser.isPresent()) {
            User user = currentUser.get();
            if(user.getAdmin()){
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }else if(adminEmails.contains(user.getEmail())){
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                user.setAdmin(true);
                changed = true;
            }
            else if(user.getProfessor()){
                authorities.add(new SimpleGrantedAuthority("ROLE_PROFESSOR"));
            }else{
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }
            if(!user.getFullName().equals(oidcUser.getFullName())){
                user.setFullName(oidcUser.getFullName());
                changed = true;
            }if(!user.getEmail().equals(oidcUser.getEmail())){
                user.setEmail(oidcUser.getEmail());
                changed = true;
            }if(!user.getGivenName().equals(oidcUser.getGivenName())){
                user.setGivenName(oidcUser.getGivenName());
                changed = true;
            }if(!user.getPictureUrl().equals(oidcUser.getPicture())){
                user.setPictureUrl(oidcUser.getPicture());
                changed = true;
            }
            if (user.getGithubId() != 0 && user.getGithubLogin() != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_GITHUB"));
            }

            if(changed){
                userRepository.save(user);
            }
        }else{
            User newUser = User.builder()
                    .googleSub(oidcUser.getSubject())
                    .fullName(oidcUser.getFullName())
                    .email(oidcUser.getEmail())
                    .givenName(oidcUser.getGivenName())
                    .pictureUrl(oidcUser.getPicture())
                    .build();
            if (adminEmails.contains(oidcUser.getEmail())) {
                newUser.setAdmin(true);
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }else{
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }
            userRepository.save(newUser);
        }
        authorities.addAll(oidcUser.getAuthorities());
        return new DefaultOidcUser(authorities, oidcUser.getIdToken(),  oidcUser.getUserInfo());
    }

}
