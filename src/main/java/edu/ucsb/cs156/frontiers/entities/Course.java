package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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
    private User creator;

    private String courseName;

    private String term;

    private String school;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
    @Fetch(FetchMode.JOIN)
    private List<CourseStaff> courseStaff;
}
