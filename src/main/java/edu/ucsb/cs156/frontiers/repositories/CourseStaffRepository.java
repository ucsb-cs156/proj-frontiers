package edu.ucsb.cs156.frontiers.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ucsb.cs156.frontiers.entities.CourseStaff;

public interface CourseStaffRepository extends JpaRepository<CourseStaff,Long>
{
    /**
     * This method returns a CourseStaff entity with a given email.
     * @param email email address of the course staff
     * @return CourseStaff entity (empty if not found)
     */
    Iterable<CourseStaff> findAllByEmail(String email);
}
