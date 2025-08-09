package edu.ucsb.cs156.frontiers.interceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import edu.ucsb.cs156.frontiers.repositories.AdminRepository;
import edu.ucsb.cs156.frontiers.repositories.InstructorRepository;

@ExtendWith(MockitoExtension.class)
public class RoleUpdateInterceptorTests {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    @InjectMocks
    private RoleUpdateInterceptor interceptor;

    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private OAuth2AuthenticationToken authentication;
    
    @Mock
    private OidcUser oidcUser;
    
    private Set<GrantedAuthority> authorities;
    private Authentication updatedAuthentication;

    @BeforeEach
    public void setUp() {
        // Create a set of authorities
        authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // Setup OidcUser with lenient to avoid UnnecessaryStubbingException
        lenient().when(oidcUser.getEmail()).thenReturn("user@example.com");

        // Setup OAuth2AuthenticationToken with lenient
        lenient().when(authentication.getPrincipal()).thenReturn(oidcUser);
        lenient().when(authentication.getAuthorities()).thenReturn(authorities);
        lenient().when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");

        // Setup SecurityContext
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        
        // Capture the updated authentication
        doAnswer(invocation -> {
            updatedAuthentication = invocation.getArgument(0);
            return null;
        }).when(securityContext).setAuthentication(any(Authentication.class));
    }

    @Test
    public void testPreHandle_UserIsAdmin() throws Exception {
        // Setup
        lenient().when(adminRepository.existsByEmail(anyString())).thenReturn(true);
        lenient().when(instructorRepository.existsByEmail(anyString())).thenReturn(false);

        // Execute
        boolean result = interceptor.preHandle(request, response, handler);

        // Verify
        assertTrue(result, "preHandle should return true");
        verify(securityContext).setAuthentication(any(Authentication.class));
        
        // Check that the updated authentication has the ROLE_ADMIN authority
        Collection<? extends GrantedAuthority> updatedAuthorities = updatedAuthentication.getAuthorities();
        boolean hasAdminRole = updatedAuthorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        assertTrue(hasAdminRole, "User should have ROLE_ADMIN");
    }

    @Test
    public void testPreHandle_UserIsInstructor() throws Exception {
        // Setup
        lenient().when(adminRepository.existsByEmail(anyString())).thenReturn(false);
        lenient().when(instructorRepository.existsByEmail(anyString())).thenReturn(true);

        // Execute
        boolean result = interceptor.preHandle(request, response, handler);

        // Verify
        assertTrue(result, "preHandle should return true");
        verify(securityContext).setAuthentication(any(Authentication.class));
        
        // Check that the updated authentication has the ROLE_INSTRUCTOR authority
        Collection<? extends GrantedAuthority> updatedAuthorities = updatedAuthentication.getAuthorities();
        boolean hasInstructorRole = updatedAuthorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_INSTRUCTOR"));
        
        assertTrue(hasInstructorRole, "User should have ROLE_INSTRUCTOR");
    }

    @Test
    public void testPreHandle_UserIsNeitherAdminNorInstructor() throws Exception {
        // Setup
        lenient().when(adminRepository.existsByEmail(anyString())).thenReturn(false);
        lenient().when(instructorRepository.existsByEmail(anyString())).thenReturn(false);

        // Execute
        boolean result = interceptor.preHandle(request, response, handler);

        // Verify
        assertTrue(result, "preHandle should return true");
        verify(securityContext).setAuthentication(any(Authentication.class));
        
        // Check that the updated authentication has only the ROLE_USER authority
        Collection<? extends GrantedAuthority> updatedAuthorities = updatedAuthentication.getAuthorities();
        
        boolean hasAdminRole = updatedAuthorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        boolean hasInstructorRole = updatedAuthorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_INSTRUCTOR"));
        
        boolean hasUserRole = updatedAuthorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"));
        
        assertTrue(!hasAdminRole, "User should not have ROLE_ADMIN");
        assertTrue(!hasInstructorRole, "User should not have ROLE_INSTRUCTOR");
        assertTrue(hasUserRole, "User should have ROLE_USER");
    }

    @Test
    public void testPreHandle_UserHasExistingRoles() throws Exception {
        // Setup
        lenient().when(adminRepository.existsByEmail(anyString())).thenReturn(true);
        lenient().when(instructorRepository.existsByEmail(anyString())).thenReturn(false);
        
        // Add existing roles to initial authorities
        authorities.clear(); // Clear existing authorities
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        authorities.add(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"));
        authorities.add(new SimpleGrantedAuthority("ROLE_GITHUB"));

        // Execute
        boolean result = interceptor.preHandle(request, response, handler);

        // Verify
        assertTrue(result, "preHandle should return true");
        verify(securityContext).setAuthentication(any(Authentication.class));
        
        // Check that the updated authentication has the correct authorities
        Collection<? extends GrantedAuthority> updatedAuthorities = updatedAuthentication.getAuthorities();
        
        boolean hasAdminRole = updatedAuthorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        boolean hasInstructorRole = updatedAuthorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_INSTRUCTOR"));
        
        boolean hasGithubRole = updatedAuthorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_GITHUB"));
        
        assertTrue(hasAdminRole, "User should have ROLE_ADMIN");
        assertTrue(!hasInstructorRole, "User should not have ROLE_INSTRUCTOR");
        assertTrue(hasGithubRole, "User should retain ROLE_GITHUB");
    }


}