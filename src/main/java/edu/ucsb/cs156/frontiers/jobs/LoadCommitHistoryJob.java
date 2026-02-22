package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.BranchId;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@Builder
@EqualsAndHashCode
public class LoadCommitHistoryJob implements JobContextConsumer {

  GithubGraphQLService githubService;
  Course course;
  List<BranchId> branches;

  @Override
  public void accept(JobContext c) throws Exception {
    c.log("Loading commit history for course: " + course.getCourseName());
    for (BranchId branchId : branches) {
      githubService.loadCommitHistory(course, branchId);
      c.log("Commit history loaded or updated for branch: " + branchId);
    }
    c.log("Done");
  }
}
