package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "INSTRUCTORS")
public class Instructor {

    @Id
    @Column(name = "EMAIL", nullable = false, unique = true, length = 255)
    private String email;
}
