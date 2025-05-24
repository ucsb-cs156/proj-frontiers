package edu.ucsb.cs156.frontiers.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import edu.ucsb.cs156.frontiers.entities.Admin;

@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {}