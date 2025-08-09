package edu.ucsb.cs156.frontiers.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.Collection;

@Component
public class RoleUpdateInterceptor implements HandlerInterceptor {
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private InstructorRepository instructorRepository;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Update user's security context on server each time the user makes HTTP request to the backend
        // If user has admin or instructor status in database, we will update their roles in security context
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            if (oauthToken.getPrincipal() instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) oauthToken.getPrincipal();
                String email = oidcUser.getEmail();
                
                if (email != null) {
                    Set<GrantedAuthority> newAuthorities = new HashSet<>();
                    Collection<? extends GrantedAuthority> currentAuthorities = authentication.getAuthorities();
                    
                    // Copy all existing authorities except ROLE_ADMIN and ROLE_INSTRUCTOR
                    currentAuthorities.stream()
                        .filter(authority -> !authority.getAuthority().equals("ROLE_ADMIN") && 
                                            !authority.getAuthority().equals("ROLE_INSTRUCTOR"))
                        .forEach(newAuthorities::add);
                    
                    // Check if user is admin or instructor and add appropriate role
                    if (adminRepository.existsByEmail(email)) {
                        newAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    } else if (instructorRepository.existsByEmail(email)) {
                        newAuthorities.add(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"));
                    }
                    
                    // Create new authentication with updated authorities
                    Authentication newAuth = new OAuth2AuthenticationToken(
                        oidcUser, 
                        newAuthorities, 
                        oauthToken.getAuthorizedClientRegistrationId()
                    );
                    
                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                }
            }
        }
        
        return true;
    }
}