# Pull Requests: PR Checklist

Here are a few tips when working on PRs. PRs will not be reviewed until all these items are taken care of:

1. **Remove commented out code.**
2. **One concern per PR:** Each PR should deal with only one concern (feature or bug fix). Smaller PRs are better.
3. **PR Title:**
   - Descriptive enough that someone familiar with the project can understand it at first glance.
   - Short enough to be readable at a glance.
4. **PR Description:**
   - Lead with a sentence about what a user or developer will notice is different (the "what" and the "why").
   - Be clear about original functionality, changes, and new functionality.
   - Include before and after screenshots where possible.
5. **Fix merge conflicts.**
6. **Deploy it and link to your deployment:**
   - For moderately complex changes, deploy to a dokku dev instance and include the link.
   - Use a dedicated dokku deployment per branch.
   - Redeploy when code changes (`dokku git:sync ...` and `dokku ps:redeploy ...`).
   - Give staff/team members admin access if needed to test.
7. **Storybook link:** If it's a frontend-only change to components not yet accessible, link to the Storybook for that PR.
8. **Start with an up-to-date branch:** Always start with a new branch that is an up-to-date copy of `main`.
9. **One branch per PR:** If waiting for a PR review, start a new branch from `main` for the next issue.
10. **Link to an issue:** Use keywords like `Closes #15` or link manually. Explain if an issue is only partially addressed.
11. **Kanban board:** Ensure the linked Issue is in the "In Review" column on the team's Kanban board.
12. **Assign the PR:** Assign to team member(s) who worked on it.
13. **Tests pass:** Test cases all pass.

**Additional Considerations:**
- Keep formatting and code changes separate.
- Don't update dependencies unnecessarily.
- Don't commit `node_modules`. Always use `git status` after `git add .`.

## Code Review Phase
- Convert "draft PR" to regular PR before review.
- Get a code review from a fellow team member.
- Get a code review from a staff member (LA, TA, or instructor).
- Address all concerns brought up in code review.
- Keep PRs small and focused (fewer than 10 files is ideal).

## PR Descriptions
- Lead with non-technical language (what an end-user would notice).
- Include whatever is needed for testing and review (steps to reproduce, sample values).
- State merge order if necessary (e.g., "Merge after PR #12").
- Include active before/after Storybook links and screenshots (for frontend).
- Include Swagger screenshots and testing advice (for backend).
