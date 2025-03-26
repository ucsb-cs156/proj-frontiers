package edu.ucsb.cs156.frontiers.jobs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;

/**
 * A simple test job that logs a message and optionally sleeps for a given amount of time.
 * This is used to test the job runner independent of any specific job code.
 */

@Builder
@AllArgsConstructor
public class TestJob implements JobContextConsumer {

  /** default constructor */
  public TestJob() {
    this.fail = false;
    this.sleepMs = 0;
  }

  private boolean fail;
  private int sleepMs;

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
