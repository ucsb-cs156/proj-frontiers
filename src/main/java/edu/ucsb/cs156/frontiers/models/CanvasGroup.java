package edu.ucsb.cs156.frontiers.models;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CanvasGroup {

  private String name;
  private Integer id;
  private List<String> members;
}
