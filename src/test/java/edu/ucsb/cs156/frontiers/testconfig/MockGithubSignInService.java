package edu.ucsb.cs156.frontiers.testconfig;

import edu.ucsb.cs156.frontiers.services.GithubSignInService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashSet;

public class MockGithubSignInService extends DefaultOAuth2UserService implements GithubSignInService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);
        return attachWiremockUser(oAuth2User);
    }

    private OAuth2User attachWiremockUser(OAuth2User oAuth2User) {
        HashSet<GrantedAuthority> authorities = new HashSet<>();
        if(oAuth2User.getAttribute("given_name") == "admin"){
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        authorities.addAll(oAuth2User.getAuthorities());
        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "sub");
    }
}
