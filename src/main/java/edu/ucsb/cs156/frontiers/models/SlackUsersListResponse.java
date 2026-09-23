package edu.ucsb.cs156.frontiers.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One page of the response from the Slack <code>users.list</code> method.
 *
 * @see <a href="https://docs.slack.dev/reference/methods/users.list">users.list</a>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackUsersListResponse {
  private boolean ok;
  private String error;
  private List<SlackUser> members;

  @JsonProperty("response_metadata")
  private ResponseMetadata responseMetadata;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ResponseMetadata {
    @JsonProperty("next_cursor")
    private String nextCursor;
  }

  /**
   * @return the cursor for the next page, or an empty string if this is the last page
   */
  public String nextCursor() {
    if (responseMetadata == null || responseMetadata.getNextCursor() == null) {
      return "";
    }
    return responseMetadata.getNextCursor();
  }
}
