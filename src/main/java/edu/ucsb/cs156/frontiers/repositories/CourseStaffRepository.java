package edu.ucsb.cs156.frontiers.repositories;

import java.util.Optional;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.ucsb.cs156.frontiers.entities.CourseStaff;

import java.util.List;

public interface CourseStaffRepository extends JpaRepository<CourseStaff,Long>
{
    List<CourseStaff> findAllByEmail(String email);
    public Iterable<CourseStaff> findByCourseId(Long courseId);
}
