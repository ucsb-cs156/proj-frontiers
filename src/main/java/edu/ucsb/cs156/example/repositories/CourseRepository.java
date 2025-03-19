package edu.ucsb.cs156.example.repositories;

import edu.ucsb.cs156.example.entities.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course,Long>
{
}
