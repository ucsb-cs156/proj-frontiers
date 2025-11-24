package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "assignment")
public class Assignment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @NotBlank(message = "Name must not be blank")
  private String name;

  @NotNull
  @Pattern(regexp = "individual|team", message = "asn_type must be 'individual' or 'team'")
  private String asn_type;

  @NotNull
  @Pattern(regexp = "public|private", message = "visibility must be 'public' or 'private'")
  private String visibility;

  @NotNull
  @Pattern(
      regexp = "read|write|maintain|admin",
      message = "permission must be 'read', 'write', 'maintain', or 'admin'")
  private String permission;
}
