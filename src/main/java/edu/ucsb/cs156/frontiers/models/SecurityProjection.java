package edu.ucsb.cs156.frontiers.models;

import java.util.List;

/** Projection for {@link edu.ucsb.cs156.frontiers.entities.Course} */
public interface SecurityProjection {

  Long getId();

  String getInstructorEmail();

  List<CourseStaffInfo> getCourseStaff();

  /** Projection for {@link edu.ucsb.cs156.frontiers.entities.CourseStaff} */
  interface CourseStaffInfo {

    String getEmail();
  }
}
