package edu.ucsb.cs156.frontiers.models;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A group in a Canvas group set, as needed to push teams to Canvas: its numeric Canvas id, its
 * name, and its current members as a map from canonical email to numeric Canvas user id.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CanvasGroupDetail {
  private Integer id;
  private String name;
  private Map<String, Integer> memberUserIdsByEmail;
}
