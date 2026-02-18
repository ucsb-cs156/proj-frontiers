package edu.ucsb.cs156.frontiers.services.jobs;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class JobContextConsumerTests {
  @Test
  public void default_getCourse_returns_null() throws Exception {

    // create a simple lambda implementation
    JobContextConsumer consumer = (c) -> {};

    // call default method
    assertNull(consumer.getCourse());
  }
}
