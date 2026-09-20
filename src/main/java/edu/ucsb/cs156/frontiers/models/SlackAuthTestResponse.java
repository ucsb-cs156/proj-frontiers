package edu.ucsb.cs156.frontiers.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from the Slack <code>auth.test</code> method. When <code>ok</code> is false, <code>error
 * </code> contains a Slack error code such as <code>invalid_auth</code>.
 *
 * @see <a href="https://docs.slack.dev/reference/methods/auth.test">auth.test</a>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackAuthTestResponse {
  private boolean ok;
  private String error;
  private String url;
  private String team;

  @JsonProperty("team_id")
  private String teamId;
}
