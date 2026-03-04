package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.DownloadRequest;
import edu.ucsb.cs156.frontiers.entities.DownloadedCommit;
import org.springframework.data.repository.CrudRepository;

public interface DownloadedCommitRepository extends CrudRepository<DownloadedCommit, String> {

  Iterable<DownloadedCommit> findByRequest(DownloadRequest downloadRequest);
}
