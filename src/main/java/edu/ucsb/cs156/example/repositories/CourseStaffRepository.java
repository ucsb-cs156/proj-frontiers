package edu.ucsb.cs156.example.repositories;

import edu.ucsb.cs156.example.entities.CourseStaff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseStaffRepository extends JpaRepository<CourseStaff,Long>
{
}
