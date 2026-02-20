package edu.ucsb.cs156.frontiers.entities;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Embeddable
public record BranchId(
    @NotNull @NotBlank String org,
    @NotNull @NotBlank String repo,
    @NotNull @NotBlank String branchName) {}
