package edu.ucsb.cs156.frontiers.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class CanvasGroupSet {
  private String name;
  private String id;
}
