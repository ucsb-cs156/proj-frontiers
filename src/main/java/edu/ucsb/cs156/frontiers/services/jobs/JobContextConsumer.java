package edu.ucsb.cs156.frontiers.services.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;

@FunctionalInterface
public interface JobContextConsumer {
  void accept(JobContext c) throws Exception;

  default Course getCourse() {
    return null;
  }
}
