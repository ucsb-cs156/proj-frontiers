package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Entity
@Table(
    name = "course_api_key",
    indexes = {
      @Index(columnList = "keyHash"),
      @Index(columnList = "created_by_id"),
      @Index(columnList = "course_id"),
    })
public class CourseApiKey {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  @ToString.Exclude
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id", nullable = false)
  @ToString.Exclude
  private User createdBy;

  @Column(nullable = false)
  private String keyHash;

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
  private Long usageCount = 0L;
}
