package edu.ucsb.cs156.frontiers.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ucsb.cs156.frontiers.entities.CourseStaff;

public interface CourseStaffRepository extends JpaRepository<CourseStaff,Long>
{
        List<CourseStaff> findAllByEmail(String email);

}
