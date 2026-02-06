package edu.ucsb.cs156.frontiers.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
public class Course {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String installationId;

  private String orgName;

  private String instructorEmail;

  private String courseName;

  private String term;

  private String school;

  @JsonIgnore private String canvasApiToken;

  private String canvasCourseId;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
  @Fetch(FetchMode.JOIN)
  @JsonIgnore
  @ToString.Exclude
  private List<CourseStaff> courseStaff;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
  @Fetch(FetchMode.JOIN)
  @JsonIgnore
  @ToString.Exclude
  private List<RosterStudent> rosterStudents;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
  @Fetch(FetchMode.JOIN)
  @JsonIgnore
  @ToString.Exclude
  private List<Team> teams;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
  @Fetch(FetchMode.JOIN)
  @JsonIgnore
  @ToString.Exclude
  private List<Section> sections;

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
    Course course = (Course) o;
    return getId() != null && Objects.equals(getId(), course.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
        : getClass().hashCode();
  }
}
