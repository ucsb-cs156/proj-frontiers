package edu.ucsb.cs156.frontiers.repositories;

import edu.ucsb.cs156.frontiers.entities.Branch;
import edu.ucsb.cs156.frontiers.entities.BranchId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface BranchRepository
    extends CrudRepository<Branch, BranchId>, JpaSpecificationExecutor<Branch> {

  List<Branch> findByIdIn(List<BranchId> ids);
}
