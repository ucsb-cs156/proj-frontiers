package edu.ucsb.cs156.frontiers.errors;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

public class NotAuthenticatedWithGoogleException extends OAuth2AuthenticationException {
    /**
     * Constructor for the exception
     *
     * @param errorCode Message for the fact that a User is currently not authenticated with Google
     */
    public NotAuthenticatedWithGoogleException(String errorCode) {
        super(errorCode);
    }
}
