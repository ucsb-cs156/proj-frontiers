package edu.ucsb.cs156.frontiers.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ucsb.cs156.frontiers.enums.OrgStatus;
import edu.ucsb.cs156.frontiers.enums.RosterStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.proxy.HibernateProxy;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_ROSTER_STUDENT_COURSE_STUDENT",
          columnNames = {"course_id", "student_id"}),
      @UniqueConstraint(
          name = "UK_ROSTER_STUDENT_COURSE_EMAIL",
          columnNames = {"course_id", "email"})
    })
public class RosterStudent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "course_id")
  private Course course;

  private String studentId;
  private String firstName;
  private String lastName;
  private String email;
  @Builder.Default private String section = "";

  @ManyToOne
  @JoinColumn(name = "user_id")
  @ToString.Exclude
  private User user;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "rosterStudent")
  @Fetch(FetchMode.JOIN)
  @JsonIgnore
  @ToString.Exclude
  private List<TeamMember> teamMembers;

  @Enumerated(EnumType.STRING)
  private RosterStatus rosterStatus;

  @Enumerated(EnumType.STRING)
  private OrgStatus orgStatus;

  private Integer githubId;
  private String githubLogin;

  public List<String> getTeams() {
    if (teamMembers == null) {
      return List.of();
    } else {
      return teamMembers.stream().map(tm -> tm.getTeam().getName()).toList();
    }
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    Class<?> oEffectiveClass =
        o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
    Class<?> thisEffectiveClass =
        this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
            : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) {
      return false;
    }
    RosterStudent that = (RosterStudent) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
