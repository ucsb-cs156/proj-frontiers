package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Represents a GitHub branch identifier with organization, repository, and branch name. regex
 * pattern is specified to prevent injection into GitHub GraphQL API.
 */
@Embeddable
public record BranchId(
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Invalid org name") @NotNull @NotBlank
        String org,
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Invalid repo name") @NotNull @NotBlank
        String repo,
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Invalid branch name") @NotNull @NotBlank
        String branchName) {}
