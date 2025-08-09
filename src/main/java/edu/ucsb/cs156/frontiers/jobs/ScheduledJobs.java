package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.repositories.CourseRepository;
import edu.ucsb.cs156.frontiers.repositories.CourseStaffRepository;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.services.OrganizationMemberService;
import edu.ucsb.cs156.frontiers.services.jobs.JobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * This class contains methods that are scheduled to run at certain times to launch particular jobs.
 *
 * <p>The value of the <code>cron</code> parameter to the <code>&#64;Scheduled</code> annotation is
 * a Spring Boot cron expression, which is similar to a Unix cron expression, but with an extra
 * field at the beginning for the seconds.
 *
 * @see <a href=
 *     "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html">Spring
 *     Cron Syntax</a>
 */
@Component("scheduledJobs")
@Slf4j
public class ScheduledJobs {

  @Autowired private JobService jobService;

  @Autowired private RosterStudentRepository rosterStudentRepository;

  @Autowired private CourseRepository courseRepository;

  @Autowired private OrganizationMemberService organizationMemberService;

  @Autowired private CourseStaffRepository courseStaffRepository;

  @Scheduled(cron = "${app.jobs.MembershipAuditJob.cron}", zone = "${spring.jackson.time-zone}")
  public void runMembershipAuditJobBasedOnCron() throws Exception {
    MembershipAuditJob job =
        MembershipAuditJob.builder()
            .rosterStudentRepository(rosterStudentRepository)
            .courseRepository(courseRepository)
            .organizationMemberService(organizationMemberService)
            .courseStaffRepository(courseStaffRepository)
            .build();

    jobService.runAsJob(job);
    log.info("runMembershipAuditJobBasedOnCron: running");
  }
}
