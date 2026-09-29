package edu.ucsb.cs156.frontiers.models;

/**
 * Warnings about the GitHub organization linked to a course, shown to those managing the course.
 *
 * @param showOrganizationAgeWarning the organization is less than a month old
 * @param showDefaultBasePermissions the organization's default repository permission is not {@code
 *     none}
 * @param showFreePlanWarning the organization is on GitHub's Free plan, on which features such as
 *     rulesets (used to require signed commits) are unavailable on private repositories
 */
public record CourseWarning(
    boolean showOrganizationAgeWarning,
    boolean showDefaultBasePermissions,
    boolean showFreePlanWarning) {}
