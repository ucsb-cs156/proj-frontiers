package edu.ucsb.cs156.frontiers.models;
import java.util.List;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;

public record LoadResult(Integer created, Integer updated, List<RosterStudent> rejected) {}
