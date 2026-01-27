package edu.ucsb.cs156.frontiers.models;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CanvasGroup {
  private Integer groupId;
  private String name;
  List<String> emails;
}
