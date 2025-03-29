package edu.ucsb.cs156.frontiers.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.services.UpdateUserService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;

@ExtendWith(MockitoExtension.class)
public class UpdateUsersJobTests {
    @Mock
    private UpdateUserService updateUserService;
    Job jobStarted = Job.builder().build();
    JobContext ctx = new JobContext(null, jobStarted);

    @Test
    void test_UpdateAllJob() throws Exception {
        var job = spy(
                UpdateAllJob.builder()
                        .updateUserService(updateUserService)
                        .build());

        job.accept(ctx);
        String expected = """
                Processing...
                Done""";

        assertEquals(expected, jobStarted.getLog());

    }

}
