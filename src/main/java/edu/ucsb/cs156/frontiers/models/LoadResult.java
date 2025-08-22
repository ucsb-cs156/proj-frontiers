package edu.ucsb.cs156.frontiers.models;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import java.util.List;

public record LoadResult(Integer created, Integer updated, List<RosterStudent> rejected) {}
