package edu.ucsb.cs156.frontiers.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A member of a Slack workspace, as returned by the Slack <code>users.list</code> method.
 *
 * @see <a href="https://docs.slack.dev/reference/objects/user-object">User object</a>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackUser {

  /** Slack's built in Slackbot user is not flagged as a bot, but is not a person either. */
  public static final String SLACKBOT_ID = "USLACKBOT";

  private String id;
  private String name;

  /** True if the account has been deactivated. */
  private boolean deleted;

  @JsonProperty("real_name")
  private String realName;

  @JsonProperty("is_bot")
  private boolean bot;

  /** True if the user has been invited to the workspace, but has not yet signed in. */
  @JsonProperty("is_invited_user")
  private boolean invitedUser;

  private Profile profile;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Profile {
    /** Only present when the token has the <code>users:read.email</code> scope. */
    private String email;

    @JsonProperty("display_name")
    private String displayName;
  }

  /**
   * @return the user's email, or null if Slack did not provide one
   */
  public String email() {
    return profile == null ? null : profile.getEmail();
  }

  /**
   * @return the user's display name, or null if Slack did not provide one
   */
  public String displayName() {
    return profile == null ? null : profile.getDisplayName();
  }

  /**
   * @return true if this is a person (not a bot, and not Slackbot)
   */
  public boolean isPerson() {
    return !bot && !SLACKBOT_ID.equals(id);
  }

  /**
   * @return true if this is a person with an account that is neither deactivated nor still waiting
   *     for an invitation to be accepted
   */
  public boolean isActivePerson() {
    return isPerson() && !deleted && !invitedUser;
  }
}
