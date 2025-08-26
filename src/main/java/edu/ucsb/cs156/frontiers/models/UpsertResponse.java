package edu.ucsb.cs156.frontiers.models;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.InsertStatus;

public record UpsertResponse(InsertStatus insertStatus, RosterStudent rosterStudent) {
  public InsertStatus getInsertStatus() {
    return insertStatus;
  }
}
