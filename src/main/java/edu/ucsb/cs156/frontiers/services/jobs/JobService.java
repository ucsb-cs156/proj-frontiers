package edu.ucsb.cs156.frontiers.services.jobs;

import edu.ucsb.cs156.frontiers.entities.Job;
import edu.ucsb.cs156.frontiers.repositories.JobsRepository;
import edu.ucsb.cs156.frontiers.services.CurrentUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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
  @Autowired private TransactionTemplate transactionTemplate;

  public Job runAsJob(JobContextConsumer jobFunction) {
    String jobName = jobFunction.getClass().getName().replace("edu.ucsb.cs156.frontiers.jobs.", "");

    Job job =
        Job.builder()
            .createdBy(currentUserService.getUser())
            .status("running")
            .jobName(jobName)
            .course(jobFunction.getCourse())
            .build();

    log.info("Starting job: {}, jobName={}", job.getId(), job.getJobName());

    jobsRepository.save(job);
    self.runJobAsync(job, jobFunction);

    return job;
  }

  /**
   * Runs a job asynchronously.
   *
   * <p>This method uses a TransactionTemplate because outside of the Spring context, you cannot
   * delete entities that are unmanaged by Hibernate. Using the transactionTemplate lambda keeps the
   * database session open and allows Hibernate to maintain it's knowledge of the object graph (i.e.
   * the entities)
   *
   * <p>To learn more, read about Hibernate and the concept of a Spring Context.
   *
   * <p>Note that using the transactionTemplate lambda means that if there is an unhandled
   * exception, either every database transactions succeeds, or all of them are rolled back.
   *
   * <p>However, the job entity metadata will still be saved.
   *
   * @param job metadata entity about the job
   * @param jobFunction runnable job function
   */
  @Async
  public void runJobAsync(Job job, JobContextConsumer jobFunction) {
    JobContext context = contextFactory.createContext(job);

    try {
      transactionTemplate.executeWithoutResult(
          status -> {
            try {
              jobFunction.accept(context);
              /*lambdas cannot throw checked exceptions
              have to repackage as a runtime exception
              to catch outside transactional boundary*/
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
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
