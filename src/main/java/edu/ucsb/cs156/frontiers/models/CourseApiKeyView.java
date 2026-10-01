package edu.ucsb.cs156.frontiers.models;

import edu.ucsb.cs156.frontiers.entities.CourseApiKey;
import java.time.ZonedDateTime;

/**
 * What an instructor may see about one of their course's API keys: everything except the key
 * itself, which is never stored and so can never be shown again. The last six characters of the key
 * (<code>keySuffix</code>) are enough to tell keys apart without compromising them.
 *
 * @param id the key's id, used to revoke it
 * @param label the name the instructor gave the key, or null
 * @param keySuffix the last six characters of the key
 * @param createdByEmail the email of the user who created the key
 * @param createdAt when the key was created
 * @param expiresAt when the key stops working
 * @param lastUsedAt when the key was last used, or null if never
 * @param usageCount how many requests have been made with the key
 * @param revoked whether the key has been revoked
 * @param status ACTIVE, EXPIRED or REVOKED, as of the time the view was built
 */
public record CourseApiKeyView(
    Long id,
    String label,
    String keySuffix,
    String createdByEmail,
    ZonedDateTime createdAt,
    ZonedDateTime expiresAt,
    ZonedDateTime lastUsedAt,
    Long usageCount,
    boolean revoked,
    Status status) {

  /** Whether a key can currently be used. A revoked key stays REVOKED even once it expires. */
  public enum Status {
    ACTIVE,
    EXPIRED,
    REVOKED
  }

  /**
   * Builds the view of a key as of the given moment.
   *
   * @param key the key
   * @param now the current time, used to decide whether the key has expired
   * @return the view
   */
  public static CourseApiKeyView from(CourseApiKey key, ZonedDateTime now) {
    Status status;
    if (key.getRevoked()) {
      status = Status.REVOKED;
    } else if (key.getExpiresAt().isBefore(now)) {
      status = Status.EXPIRED;
    } else {
      status = Status.ACTIVE;
    }
    return new CourseApiKeyView(
        key.getId(),
        key.getLabel(),
        key.getKeySuffix(),
        key.getCreatedBy().getEmail(),
        key.getCreatedAt(),
        key.getExpiresAt(),
        key.getLastUsedAt(),
        key.getUsageCount(),
        key.getRevoked(),
        status);
  }
}
