package edu.ucsb.cs156.frontiers.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.entities.User;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

public class JobDTOTests {

  @Test
  public void test_fromEntity_all_fields_present() {
    ZonedDateTime now = ZonedDateTime.now();

    User user = User.builder().email("user@example.org").build();

    Course course = new Course();
    course.setCourseName("CMPSC 156");

    Job job =
        Job.builder()
            .id(1L)
            .status("completed")
            .jobName("TestJob")
            .createdAt(now)
            .updatedAt(now)
            .createdBy(user)
            .course(course)
            .log("This is a test log")
            .build();

    JobDTO dto = JobDTO.fromEntity(job);

    assertEquals(1L, dto.getId());
    assertEquals("completed", dto.getStatus());
    assertEquals("TestJob", dto.getJobName());
    assertEquals(now, dto.getCreatedAt());
    assertEquals(now, dto.getUpdatedAt());
    assertEquals("user@example.org", dto.getUserEmail());
    assertEquals("CMPSC 156", dto.getCourseName());
    assertEquals("This is a test log", dto.getLog());
  }

  @Test
  public void test_fromEntity_when_createdBy_is_null() {

    Job job = Job.builder().id(2L).status("running").jobName("NoUserJob").createdBy(null).build();

    JobDTO dto = JobDTO.fromEntity(job);

    assertNull(dto.getUserEmail());
  }

  @Test
  public void test_fromEntity_when_course_is_null() {

    Job job = Job.builder().id(3L).status("queued").jobName("NoCourseJob").course(null).build();

    JobDTO dto = JobDTO.fromEntity(job);

    assertNull(dto.getCourseName());
  }

  @Test
  public void test_fromEntity_when_log_is_null() {

    Job job = Job.builder().id(4L).status("started").jobName("NoLogJob").log(null).build();

    JobDTO dto = JobDTO.fromEntity(job);

    assertNull(dto.getLog());
  }
}
