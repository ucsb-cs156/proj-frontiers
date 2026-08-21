package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.DownloadRequest;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@Builder
@EqualsAndHashCode
public class CommitDownloadRequestJob implements JobContextConsumer {

  GithubGraphQLService githubService;
  DownloadRequest request;

  @Override
  public String getScopeType() {
    return "course";
  }

  @Override
  public Long getScopeId() {
    return request.getCourse().getId();
  }

  @Override
  public void accept(JobContext c) throws Exception {
    c.log("Starting download for course " + request.getCourse().getCourseName());
    githubService.downloadCommitHistory(request);
    c.log("Download completed successfully");
  }
}
