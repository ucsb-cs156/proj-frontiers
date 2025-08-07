package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
}