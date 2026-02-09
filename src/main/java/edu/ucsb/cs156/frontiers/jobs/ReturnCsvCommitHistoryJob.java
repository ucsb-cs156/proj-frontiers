package edu.ucsb.cs156.frontiers.jobs;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.models.Commit;
import edu.ucsb.cs156.frontiers.models.CommitHistory;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.io.StringWriter;
import java.io.Writer;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@Builder
@EqualsAndHashCode
public class ReturnCsvCommitHistoryJob implements JobContextConsumer {

  GithubGraphQLService githubService;
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
    try (Writer writer = new StringWriter()) {
      StatefulBeanToCsv<Commit> beanToCsv =
          new StatefulBeanToCsvBuilder<Commit>(writer)
              .withEscapechar(CSVWriter.DEFAULT_ESCAPE_CHARACTER)
              .withSeparator(',')
              .build();
      beanToCsv.write(history.getCommits());
    }
  }
}
