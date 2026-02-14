package edu.ucsb.cs156.frontiers.jobs;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.models.Commit;
import edu.ucsb.cs156.frontiers.models.CommitHistory;
import edu.ucsb.cs156.frontiers.redis.CommitCsvResult;
import edu.ucsb.cs156.frontiers.redis.JobResultRepository;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@Builder
@EqualsAndHashCode
public class ReturnCsvCommitHistoryJob implements JobContextConsumer {

  GithubGraphQLService githubService;
  JobResultRepository jobResultRepository;
  Course course;
  String owner;
  String repo;
  Integer count;
  String branch;

  @Override
  public void accept(JobContext c) throws Exception {
    c.log("Returning CSV commit history for course: " + course.getCourseName());
    CommitHistory history = githubService.returnCommitHistory(course, owner, repo, branch, count);
    c.log("CSV commit history returned.");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (OutputStreamWriter writer = new OutputStreamWriter(baos, "UTF-8"); ) {
      StatefulBeanToCsv<Commit> beanToCsv =
          new StatefulBeanToCsvBuilder<Commit>(writer)
              .withEscapechar(CSVWriter.DEFAULT_ESCAPE_CHARACTER)
              .withSeparator(',')
              .build();
      beanToCsv.write(history.getCommits());
      writer.flush();
      CommitCsvResult result =
          CommitCsvResult.builder()
              .jobId(c.getId())
              .jobData(CommitCsvResult.JobData.builder().csvData(baos.toByteArray()).build())
              .build();
      jobResultRepository.save(result);
    }
  }
}
