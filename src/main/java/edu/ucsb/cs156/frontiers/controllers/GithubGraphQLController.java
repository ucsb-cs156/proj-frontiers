package edu.ucsb.cs156.frontiers.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.GitHubGraphQLService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@Tag(name = "GithubGraphQL")
@RequestMapping("/api/github/graphql/")
@RestController
@Slf4j
public class GithubGraphQLController extends ApiController {

    private final GitHubGraphQLService gitHubGraphQLService;
    private final CourseRepository courseRepository;

    public GithubGraphQLController(
            @Autowired GitHubGraphQLService gitHubGraphQLService,
            @Autowired CourseRepository courseRepository) {
        this.gitHubGraphQLService = gitHubGraphQLService;
        this.courseRepository = courseRepository;
    }

    /**
     * Return default branch name for a given repository.
     * @param courseId the id of the course whose installation is being used for credentails
     * @param owner the owner of the repository
     * @param repo the name of the repository
     * @return the default branch name
     */

    @Operation(summary = "Get default branch name")
    @PreAuthorize("hasRole('ROLE_ADMIN') || hasRole('ROLE_INSTRUCTOR')")
    @GetMapping("defaultBranchName")
    public String getDefaultBranchName(
       @Parameter Long courseId,
       @Parameter String owner,
       @Parameter String repo) throws Exception {
        log.info("getDefaultBranchName called with courseId: {}, owner: {}, repo: {}", courseId, owner, repo);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));

        log.info("Found course: {}", course);

        if (!isCurrentUserAdmin() && !(course.getCreator().getId() == getCurrentUser().getUser().getId())) {
            throw new EntityNotFoundException(Course.class, courseId);
        }
  
        log.info("Current user is authorized to access course: {}", course.getId());

        Mono<String> result = this.gitHubGraphQLService.getDefaultBranchName(course, owner, repo);

        log.info("Result from getDefaultBranchName: {}", result);
        
        return result.blockOptional()
                .orElseThrow(() -> new IllegalArgumentException("Default branch not found for repository: " + owner + "/" + repo));

    }


}
