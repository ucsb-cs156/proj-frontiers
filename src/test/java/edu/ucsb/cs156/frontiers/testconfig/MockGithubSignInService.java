package edu.ucsb.cs156.frontiers.testconfig;

import edu.ucsb.cs156.frontiers.services.GithubSignInService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class MockGithubSignInService implements GithubSignInService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        return null;
    }
}
