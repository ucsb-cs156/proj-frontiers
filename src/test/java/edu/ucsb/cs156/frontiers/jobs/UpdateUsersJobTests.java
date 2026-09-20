package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.jobs.entities.Job;
import edu.ucsb.cs156.jobs.services.JobContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateUsersJobTests {
  @Mock private UpdateUserService updateUserService;
  Job jobStarted = Job.builder().build();
  JobContext ctx = new JobContext(null, jobStarted);

  @Test
  public void test_getScope_returnsUnscoped() {
    UpdateAllJob job = UpdateAllJob.builder().updateUserService(updateUserService).build();
    assertNull(job.getScopeType());
    assertNull(job.getScopeId());
  }

  @Test
  void test_UpdateAllJob() throws Exception {
    var job = spy(UpdateAllJob.builder().updateUserService(updateUserService).build());

    job.accept(ctx);
    String expected = """
                Processing...
                Done""";

    assertEquals(expected, jobStarted.getLog());
    verify(updateUserService, times(1)).attachRosterStudentsAllUsers();
  }
}
