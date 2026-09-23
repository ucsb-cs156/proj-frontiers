package edu.ucsb.cs156.frontiers.models;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Canvas group set, as needed to push teams to Canvas: its numeric Canvas id (needed by the
 * Canvas REST and mutation APIs, which do not take the GraphQL relay id), its name and its groups.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CanvasGroupSetDetail {
  private Integer id;
  private String name;
  private List<CanvasGroupDetail> groups;
}
