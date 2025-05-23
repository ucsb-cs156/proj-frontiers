package edu.ucsb.cs156.frontiers.services;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface GithubSignInService extends OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    @Override
    OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException;
}
