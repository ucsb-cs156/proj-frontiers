package edu.ucsb.cs156.frontiers.services;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public interface GoogleSignInService extends OAuth2UserService<OidcUserRequest, OidcUser> {
    @Override
    OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException;

}
