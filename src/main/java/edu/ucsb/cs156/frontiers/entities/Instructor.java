package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing an instructor.
 * Uses email as the primary key.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Instructor {
    @Id
    private String email;
}