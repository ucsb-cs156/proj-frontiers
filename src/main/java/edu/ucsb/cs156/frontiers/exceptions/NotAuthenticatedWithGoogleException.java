package edu.ucsb.cs156.frontiers.exceptions;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

public class NotAuthenticatedWithGoogleException extends OAuth2AuthenticationException {
    public NotAuthenticatedWithGoogleException(String errorCode) {
        super(errorCode);
    }
}
