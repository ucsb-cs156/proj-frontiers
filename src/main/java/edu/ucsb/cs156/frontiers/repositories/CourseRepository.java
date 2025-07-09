package edu.ucsb.cs156.frontiers.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ucsb.cs156.frontiers.entities.Course;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course,Long>
{

    Optional<Course> findByInstallationId(String installationId);
    List<Course> findByCreatorId(Long userId);
}
