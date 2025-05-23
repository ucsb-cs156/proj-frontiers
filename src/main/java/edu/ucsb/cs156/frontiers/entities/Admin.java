package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "ADMINS")
public class Admin {

    @Id
    private String email;
}