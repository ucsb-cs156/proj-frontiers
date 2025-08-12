package edu.ucsb.cs156.frontiers.services.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.repositories.JobsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

public class JobContextFactoryTests {
  @Mock private JobsRepository jobsRepository;

  @InjectMocks private JobContextFactory contextFactory;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void factory_returns_jobContext() {
    Job job = mock(Job.class);
    try (MockedConstruction<JobContext> mockedConstruction =
        mockConstruction(
            JobContext.class,
            (mock, context) -> {
              assertEquals(jobsRepository, context.arguments().getFirst());
              assertEquals(job, context.arguments().get(1));
            })) {
      JobContext createdContext = contextFactory.createContext(job);
      JobContext constructedMock = mockedConstruction.constructed().getFirst();
      assertEquals(createdContext, constructedMock);
    }
  }
}
