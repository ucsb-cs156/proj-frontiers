package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import lombok.Builder;

@Builder
public class UpdateAllJob implements JobContextConsumer {

  private final UpdateUserService updateUserService;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Processing...");
    updateUserService.attachRosterStudentsAllUsers();
    ctx.log("Done");
  }
}
