package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.*;
import lombok.*;

@Data

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "ADMINS")
public class Admin {

    @Id
    @Column(nullable = false, unique = true)
    private String email;

}