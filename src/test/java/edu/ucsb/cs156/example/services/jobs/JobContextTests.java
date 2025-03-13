package edu.ucsb.cs156.example.services.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import edu.ucsb.cs156.example.entities.Job;
import org.junit.jupiter.api.Test;


public class JobContextTests {
  @Test
  public void when_jobs_repository_is_null_does_not_save() throws Exception {

    // arrange

    Job job1 = Job.builder().build();
    JobContext ctx = new JobContext(null, job1);

    // act
    ctx.log("This is a log message");

    // assert
    assertEquals("This is a log message", job1.getLog());
  }

}
