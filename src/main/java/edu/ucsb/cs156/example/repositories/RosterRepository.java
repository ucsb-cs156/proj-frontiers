package edu.ucsb.cs156.example.repositories;

import edu.ucsb.cs156.example.entities.Roster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RosterRepository extends JpaRepository<Roster, Long>
{
}
