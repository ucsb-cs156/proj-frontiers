package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.DokkuAccountTranslation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link DokkuAccountTranslation}, keyed by email. */
@Repository
public interface DokkuAccountTranslationRepository
    extends JpaRepository<DokkuAccountTranslation, String> {
  List<DokkuAccountTranslation> findAllByOrderByEmailAsc();
}
