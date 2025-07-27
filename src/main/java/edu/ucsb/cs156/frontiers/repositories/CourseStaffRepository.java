package edu.ucsb.cs156.frontiers.repositories;

import java.util.Optional;

import edu.ucsb.cs156.frontiers.entities.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.ucsb.cs156.frontiers.entities.CourseStaff;

import java.util.List;

public interface CourseStaffRepository extends JpaRepository<CourseStaff,Long>
{
    /**
     * This method returns a CourseStaff entity with a given email.
     * @param email email address of the course staff
     * @return List of CourseStaff (empty if not found)
     */  
    List<CourseStaff> findAllByEmail(String email);
    /**
     * This method returns a list of CourseStaff entities associated with a given course ID.
     * @param courseId ID of the course
     * @return Iterable of CourseStaff (empty if not found)
     */
    public Iterable<CourseStaff> findByCourseId(Long courseId);
    /**
     * This method returns a CourseStaff entity with a given email and course.
     * @param email email address of the course staff
     * @param course the course associated with the course staff
     * @return Optional of CourseStaff (empty if not found)
     */
    Optional<CourseStaff> findByEmailAndCourse(String email, Course course);

    Optional<CourseStaff> findByCourseAndGithubLogin(Course course, String githubLogin);
}
