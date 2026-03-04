package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.DownloadRequest;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@Builder
@EqualsAndHashCode
public class CommitDownloadRequestJob implements JobContextConsumer {

  GithubGraphQLService githubService;
  DownloadRequest request;

  @Override
  public void accept(JobContext c) throws Exception {
    c.log("Starting download for course " + request.getCourse().getCourseName());
    githubService.downloadCommitHistory(request);
    c.log("Download completed successfully");
  }
}
