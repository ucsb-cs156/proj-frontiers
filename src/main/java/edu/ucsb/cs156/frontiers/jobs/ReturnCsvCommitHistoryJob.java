package edu.ucsb.cs156.frontiers.jobs;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.mongo.documents.Commit;
import edu.ucsb.cs156.frontiers.mongo.documents.CommitHistory;
import edu.ucsb.cs156.frontiers.mongo.repositories.CommitRepository;
import edu.ucsb.cs156.frontiers.services.GithubGraphQLService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.io.StringWriter;
import java.io.Writer;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Builder
@EqualsAndHashCode
public class ReturnCsvCommitHistoryJob implements JobContextConsumer {

  GithubGraphQLService githubService;
  Course course;
  String owner;
  String repo;
  Integer count;
  String branch;
  CommitRepository commitRepository;

  @Override
  public void accept(JobContext c) throws Exception {
    c.log("Returning CSV commit history for course: " + course.getCourseName());
    CommitHistory mongoBranch = githubService.updateCommitHistory(course, owner, repo, branch);
    Page<Commit> history =
        commitRepository.findByParentBranch(
            mongoBranch.getId(), PageRequest.of(0, count, Sort.by("committedTime").descending()));
    c.log("CSV commit history returned.");
    try (Writer writer = new StringWriter()) {
      StatefulBeanToCsv<Commit> beanToCsv =
          new StatefulBeanToCsvBuilder<Commit>(writer)
              .withEscapechar(CSVWriter.DEFAULT_ESCAPE_CHARACTER)
              .withSeparator(',')
              .build();
      beanToCsv.write(history.stream());
    }
  }
}
