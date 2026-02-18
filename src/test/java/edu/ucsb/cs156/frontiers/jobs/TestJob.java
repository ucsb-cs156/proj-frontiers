package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import lombok.Builder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Builder
public class TestJob implements JobContextConsumer {

  private boolean fail;
  private int sleepMs;
  private Course course;

  @Override
  public Course getCourse() {
    return this.course;
  }

  @Override
  public void accept(JobContext ctx) throws Exception {
    // Ensure this is not null
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    ctx.log("Hello World! from test job!");
    Thread.sleep(sleepMs);
    if (fail) {
      throw new Exception("Fail!");
    }
    ctx.log("Goodbye from test job!");
  }
}
