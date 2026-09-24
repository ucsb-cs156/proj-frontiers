package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps an email address to the dokku username to use for it, for the case where the dokku username
 * is not simply the part of the email before the {@code @}. Rows are global, not per-course: the
 * same person has the same dokku username in every course.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "dokku_account_translation")
public class DokkuAccountTranslation {
  /** The email address, in canonical form (see CanonicalFormConverter). */
  @Id private String email;

  /** The dokku username to use for this email instead of the part before the {@code @}. */
  @Column(nullable = false)
  private String username;
}
