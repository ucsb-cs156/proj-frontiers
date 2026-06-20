package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Entity
@Table(name = "api_course_key")
public class ApiCourseKey {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  @ToString.Exclude
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id", nullable = false)
  @ToString.Exclude
  private User createdBy;

  @Column(nullable = false)
  private String keyHash;

  @Column(nullable = false)
  private String salt;

  @Column(nullable = false)
  private String keySuffix;

  @Column(nullable = false)
  private ZonedDateTime createdAt;

  @Column(nullable = false)
  private ZonedDateTime expiresAt;

  @Builder.Default
  @Column(nullable = false)
  private boolean revoked = false;

  private ZonedDateTime lastUsedAt;

  @Builder.Default
  @Column(nullable = false)
  private long usageCount = 0;

  public boolean isActive(ZonedDateTime now) {
    return !revoked && now.isBefore(expiresAt);
  }
}
