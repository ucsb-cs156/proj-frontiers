package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import lombok.Builder;

@Builder
public class UpdateAllJob implements JobContextConsumer {

  private final UpdateUserService updateUserService;

  // Unscoped: getScopeType()/getScopeId() default to null, same as the old getCourse() did.

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Processing...");
    updateUserService.attachRosterStudentsAllUsers();
    ctx.log("Done");
  }
}
