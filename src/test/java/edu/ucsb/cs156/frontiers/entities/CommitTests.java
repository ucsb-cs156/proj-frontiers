package edu.ucsb.cs156.frontiers.entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

public class CommitTests {

  @Test
  public void no_null_pointer_on_null_json_nodes() {
    Commit commit = Commit.builder().build();
    assertDoesNotThrow(() -> commit.setAuthor(null));
    assertDoesNotThrow(() -> commit.setIsMergeCommit(null));
    assertDoesNotThrow(() -> commit.setCommitter(null));
    ObjectNode hasTwoParents = JsonNodeFactory.instance.objectNode();
    NumericNode hasTwoParentsNode = JsonNodeFactory.instance.numberNode(2);
    hasTwoParents.set("totalCount", hasTwoParentsNode);
    commit.setIsMergeCommit(hasTwoParents);
    assertTrue(commit.getIsMergeCommit());
    NumericNode hasOneParentNode = JsonNodeFactory.instance.numberNode(1);
    ObjectNode hasOneParent = JsonNodeFactory.instance.objectNode();
    hasOneParent.set("totalCount", hasOneParentNode);
    commit.setIsMergeCommit(hasOneParent);
    assertFalse(commit.getIsMergeCommit());
  }
}
