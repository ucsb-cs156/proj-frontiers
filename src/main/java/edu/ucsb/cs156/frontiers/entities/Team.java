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
@Table(name = "team",
uniqueConstraints = @UniqueConstraint(columnNames = {"name", "course_id"}))
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonIgnore
    @ToString.Exclude
    private Course course;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "team")
    @Fetch(FetchMode.JOIN)
    private List<TeamMember> teamMembers;
}