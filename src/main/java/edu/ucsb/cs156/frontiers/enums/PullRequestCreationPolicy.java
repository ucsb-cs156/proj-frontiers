package edu.ucsb.cs156.frontiers.enums;

/**
 * Who may open pull requests against a GitHub repository: the values of the repository setting
 * {@code pull_request_creation_policy}.
 */
public enum PullRequestCreationPolicy {
  /** Only users with at least Write access to the repository. */
  COLLABORATORS_ONLY("collaborators_only"),
  /** Anyone who can see the repository. */
  ALL("all");

  private final String githubValue;

  PullRequestCreationPolicy(String githubValue) {
    this.githubValue = githubValue;
  }

  /** The value that GitHub's REST API uses for this policy. */
  public String getGithubValue() {
    return githubValue;
  }
}
