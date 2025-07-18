package edu.ucsb.cs156.frontiers.services.jobs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.repositories.JobsRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;

@Service
public class JobService {
  @Autowired private JobsRepository jobsRepository;

  @Autowired private CurrentUserService currentUserService;

  @Autowired private JobContextFactory contextFactory;

  @Lazy @Autowired private JobService self;

  public Job runAsJob(JobContextConsumer jobFunction) {
    Job job = Job.builder().createdBy(currentUserService.getUser()).status("running").build();

    jobsRepository.save(job);
    self.runJobAsync(job, jobFunction);

    return job;
  }

  @Async
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
