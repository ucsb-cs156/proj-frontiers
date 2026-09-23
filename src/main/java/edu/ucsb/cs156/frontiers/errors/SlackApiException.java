package edu.ucsb.cs156.frontiers.errors;

/** Thrown when a call to the Slack Web API fails; the message is the Slack error code. */
public class SlackApiException extends RuntimeException {
  public SlackApiException(String error) {
    super(error);
  }
}
