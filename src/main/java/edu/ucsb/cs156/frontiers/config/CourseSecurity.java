package edu.ucsb.cs156.frontiers.config;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.models.CurrentUser;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

@Slf4j
@Component("CourseSecurity")
public class CourseSecurity {
    private final CurrentUserService currentUserService;
    private final RoleHierarchy roleHierarchy;
    private final CourseRepository courseRepository;
    public CourseSecurity(CurrentUserService currentUserService, RoleHierarchy roleHierarchy, CourseRepository courseRepository){
        this.currentUserService = currentUserService;
        this.roleHierarchy = roleHierarchy;
        this.courseRepository = courseRepository;
    }

    @PreAuthorize("hasRole('ROLE_INSTRUCTOR')")
    public Boolean hasManagePermissions(MethodSecurityExpressionOperations operations, Long courseId){
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Collection<? extends GrantedAuthority> authorities = roleHierarchy.getReachableGrantedAuthorities(currentUser.getRoles());
        if(authorities.stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"))){
            return true;
        }else {
            Optional<Course> course = courseRepository.findById(courseId);
            if(course.isEmpty()){
                return true;
            }
            return currentUser.getUser().getEmail().equals(course.get().getInstructorEmail());
        }
    }
}
