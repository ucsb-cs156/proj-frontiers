package edu.ucsb.cs156.frontiers.security;

import edu.ucsb.cs156.frontiers.config.SecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class DummyCourseSecurity {
    @PreAuthorize("@CourseSecurity.hasManagePermissions(#root, #courseId)")
    public void loadCourse(Long courseId){
        /*
        This method simply exists to add the preauthorization annotation so that the method can be tested directly.
         */
    }
    @Bean
    public static RoleHierarchy loadedRoleHierarchy(){
        return SecurityConfig.roleHierarchy();
    }
}
