package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.Branch;
import edu.ucsb.cs156.frontiers.entities.BranchId;
import edu.ucsb.cs156.frontiers.entities.Commit;
import edu.ucsb.cs156.frontiers.models.CommitDto;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface CommitRepository
    extends CrudRepository<Commit, Long>, JpaSpecificationExecutor<Commit> {

  @BatchSize(size = 200)
  List<CommitDto> findByBranchIdInAndCommitTimeBetweenAndIsMergeCommitEquals(
      List<BranchId> branchIds, Instant start, Instant end, boolean isMergeCommit);

  @BatchSize(size = 200)
  List<CommitDto> findByBranchIdInAndCommitTimeBetween(
      List<BranchId> branchIds, Instant start, Instant end);

  Stream<Commit> streamByBranch(Branch branch);

  List<Commit> findByBranch(Branch branch);

  boolean existsByBranchAndSha(Branch branch, String sha);

  @BatchSize(size = 100)
  @Override
  <S extends Commit> Iterable<S> saveAll(Iterable<S> entities);
}
