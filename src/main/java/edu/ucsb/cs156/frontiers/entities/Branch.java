package edu.ucsb.cs156.frontiers.entities;

import com.opencsv.bean.CsvIgnore;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a branch in a repository. Uses an embedded id because it allows us to easily reference
 * other entities that need to know about the branch, such as commits.
 *
 * <p>This entity purposefully does not include a list of commits because it is intended to be a
 * lightweight bookkeeping entity for easily tracking the last time commit data was updated, and
 * also acts as a lifecycle owner for commits. When it is deleted, all the commits associated with
 * it will be cascade deleted.
 *
 * <p>In general, this entity should not be returned to a user directly -- instead, use a DTO to
 * project it. See {@link edu.ucsb.cs156.frontiers.models.CommitDto} as an example.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "branches")
public class Branch {

  @EmbeddedId private BranchId id;

  @CsvIgnore private Instant retrievedTime;
}
