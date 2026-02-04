package edu.ucsb.cs156.frontiers.services.jobs;

import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.repositories.JobsRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class JobService {
  @Autowired private JobsRepository jobsRepository;

  @Autowired private CurrentUserService currentUserService;

  @Autowired private JobContextFactory contextFactory;

  /*
   * This is a self-referential bean to allow for async method calls within the same class.
   */
  @Lazy @Autowired private JobService self;

  public Job runAsJob(JobContextConsumer jobFunction) {
    String jobName = jobFunction.getClass().getName().replace("edu.ucsb.cs156.frontiers.jobs.", "");

    Job job =
        Job.builder()
            .createdBy(currentUserService.getUser())
            .status("running")
            .jobName(jobName)
            .build();

    log.info("Starting job: {}, jobName={}", job.getId(), job.getJobName());

    jobsRepository.save(job);
    self.runJobAsync(job, jobFunction);

    return job;
  }

  @Async
  @Transactional
  public void runJobAsync(Job job, JobContextConsumer jobFunction) {
    JobContext context = contextFactory.createContext(job);

    try {
      jobFunction.accept(context);
    } catch (Exception e) {
      job.setStatus("error");
      context.log(e.getMessage());
      return;
    }

    job.setStatus("complete");
    jobsRepository.save(job);
  }

  public String getJobLogs(Long jobId) {
    Job job =
        jobsRepository
            .findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found"));

    String log = job.getLog();
    return log != null ? log : "";
  }
}
