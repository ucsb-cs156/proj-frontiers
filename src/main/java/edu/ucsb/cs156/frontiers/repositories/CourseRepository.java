package edu.ucsb.cs156.frontiers.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ucsb.cs156.frontiers.entities.Course;

public interface CourseRepository extends JpaRepository<Course,Long>
{
    
}
