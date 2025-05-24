package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
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
