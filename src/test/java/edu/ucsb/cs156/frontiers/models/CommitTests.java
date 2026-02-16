package edu.ucsb.cs156.frontiers.models;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import edu.ucsb.cs156.frontiers.mongo.documents.Commit;
import org.junit.jupiter.api.Test;

public class CommitTests {

  @Test
  public void no_null_pointer_on_null_json_nodes() {
    Commit commit = Commit.builder().build();
    assertDoesNotThrow(() -> commit.setAuthor(null));
    assertDoesNotThrow(() -> commit.setCommitter(null));
  }
}
