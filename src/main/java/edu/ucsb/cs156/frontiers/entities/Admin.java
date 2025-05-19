package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Builder
@Table(name = "admins")
public class Admin {
    @Id
    private String email;
}
