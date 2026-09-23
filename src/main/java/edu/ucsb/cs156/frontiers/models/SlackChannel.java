package edu.ucsb.cs156.frontiers.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A channel of a Slack workspace, as returned by the Slack <code>conversations.list</code> and
 * <code>conversations.create</code> methods.
 *
 * @see <a href="https://docs.slack.dev/reference/objects/conversation-object">Conversation
 *     object</a>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackChannel {
  private String id;
  private String name;

  @JsonProperty("is_archived")
  private boolean archived;
}
