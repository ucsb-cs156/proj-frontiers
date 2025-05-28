package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

@Data
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

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    @ToString.Exclude
    private User creator;

    private String courseName;

    private String term;

    private String school;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
    @Fetch(FetchMode.JOIN)
    @JsonIgnore
    @ToString.Exclude
    private List<CourseStaff> courseStaff;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "course")
    @Fetch(FetchMode.JOIN)
    @JsonIgnore
    @ToString.Exclude
    private List<RosterStudent> rosterStudents;
}
