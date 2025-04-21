package edu.ucsb.cs156.frontiers.controllers;


import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.errors.EntityNotFoundException;
import edu.ucsb.cs156.frontiers.errors.NoLinkedOrganizationException;
import edu.ucsb.cs156.frontiers.jobs.CreateStudentRepositoriesJob;
import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;


@Tag(name = "Repository Controller")
@RestController
@RequestMapping("/api/repos")
public class RepositoryController extends ApiController {

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    JobService jobService;

    @Autowired
    CourseRepository courseRepository;

    @PostMapping("/createRepos")
    @PreAuthorize("hasRole('ROLE_PROFESSOR')")
    public Job createRepos(@RequestParam Long courseId, @RequestParam String repoPrefix, @RequestParam Optional<Boolean> isPrivate) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new EntityNotFoundException(Course.class, courseId));
        if (getCurrentUser().getUser().getId() == course.getCreator().getId()) {
            if (course.getOrgName() == null || course.getInstallationId() == null) {
                throw new NoLinkedOrganizationException(course.getCourseName());
            } else {
                CreateStudentRepositoriesJob job = CreateStudentRepositoriesJob.builder()
                        .repositoryPrefix(repoPrefix)
                        .isPrivate(isPrivate.orElse(false))
                        .repositoryService(repositoryService)
                        .course(course)
                        .build();
                return jobService.runAsJob(job);
            }
        } else {
            throw new AccessDeniedException("You do not have permission to create student repositories on this course");
        }
    }
}
