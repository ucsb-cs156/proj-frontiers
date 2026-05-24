# DownloadStaffCSV Epic Plan

Local planning file for Frontiers issue `#16`:
- GitHub issue: `EPIC: DownloadStaffCSV`
- Scope: make `Download Staff CSV` work on the Staff tab
- Keep this file out of Git history and PRs

## Naming Convention

Use `DownloadStaffCSV` at the front of every issue title, commit message, and PR title.

Required prefixes:
- Commit messages: `PM - DownloadStaffCSV: ...`
- PR titles: `PM - DownloadStaffCSV: ...`

Recommended pattern:
- `PM - DownloadStaffCSV: <single concern>`

Examples:
- `PM - DownloadStaffCSV: add CourseStaff CSV DTO and service`
- `PM - DownloadStaffCSV: add course staff CSV endpoint`
- `PM - DownloadStaffCSV: enable staff tab download button`
- `PM - DownloadStaffCSV: add staff CSV help docs and help link`

## Epic Summary

Issue `#16` asks for the disabled `Download Staff CSV` button on the Instructor/Staff course view to be implemented properly.

The finished epic should provide:
- a backend endpoint for downloading course staff as CSV
- frontend wiring so the Staff tab button downloads the CSV
- help-page documentation for the downloaded CSV format
- tests for backend and frontend behavior

Out of scope:
- staff CSV upload
- changing the disabled `Upload CSV Roster` button
- unrelated Staff tab cleanup or refactors

## Issue Breakdown

The goal is to keep each issue focused on one concern so each PR stays small and reviewable, while still being meaningful enough to avoid a trivial 1-function issue.

### Issue 1

Title:
- `DownloadStaffCSV: Create DTO/service for CourseStaff CSV downloads`

Purpose:
- Add the internal backend pieces needed to export course staff as CSV rows.

Changes:
- Add a flat DTO/record for a course staff CSV row
- Add a service that fetches `CourseStaff` by `courseId`
- Add CSV writer creation for that DTO
- Define and lock the export column order

Why this is a separate issue:
- One backend preparation concern
- No endpoint, no frontend, no docs

Acceptance criteria:
- DTO is flat and does not serialize nested `course` or `user`
- CSV fields are exactly:
  - `id`
  - `courseId`
  - `userId`
  - `firstName`
  - `lastName`
  - `email`
  - `orgStatus`
  - `githubId`
  - `githubLogin`
  - `role`
- `userId` is `0` when no linked user exists

Branch:
- `pm-downloadstaffcsv-dto-service`

Commit / PR title:
- `PM - DownloadStaffCSV: add CourseStaff CSV DTO and service`

Dokku deployment:
- Not strictly needed unless you want to validate later stacked work on the same branch

Validation:
- Confirm DTO/service compiles
- Confirm export order is explicit and stable

### Issue 2

Title:
- `DownloadStaffCSV: Add backend endpoint for downloading course staff as CSV`

Purpose:
- Add the public backend CSV download endpoint.

Changes:
- Extend `CSVDownloadsController`
- Add `GET /api/csv/coursestaff?courseId={id}`
- Check that the course exists
- Stream CSV output using the DTO/service from Issue 1
- Set `Content-Disposition` and `text/csv` headers

Why this is a separate issue:
- One backend API concern
- Small enough to review independently

Acceptance criteria:
- Endpoint path is `/api/csv/coursestaff`
- Requires manage-level permissions for the course
- Returns `text/csv`
- Returns attachment filename `{courseName}_staff.csv`
- Unknown course id returns `404`

Branch:
- `pm-downloadstaffcsv-endpoint`

Commit / PR title:
- `PM - DownloadStaffCSV: add course staff CSV endpoint`

Dokku deployment:
- Recommended

Validation:
- Open endpoint in browser while logged in as an authorized user
- Confirm CSV downloads instead of rendering JSON
- Confirm unauthorized/invalid course behavior

### Issue 3

Title:
- `DownloadStaffCSV: Add controller tests for course staff CSV download`

Purpose:
- Add focused controller tests for the endpoint.

Changes:
- Add tests covering successful CSV download
- Verify exact response body
- Verify `Content-Type`
- Verify `Content-Disposition`
- Verify invalid course path
- Cover CSV writer failure path if it fits existing test patterns cleanly

Why this is a separate issue:
- Testing-only concern
- Keeps endpoint logic PR smaller
- Easier code review

Acceptance criteria:
- Tests verify exact CSV content for at least one sample staff row
- Tests verify filename header
- Tests verify `404` for unknown course

Branch:
- `pm-downloadstaffcsv-endpoint-tests`

Commit / PR title:
- `PM - DownloadStaffCSV: add tests for course staff CSV endpoint`

Dokku deployment:
- Not required

Validation:
- Run targeted backend tests
- Make sure they pass without modifying unrelated behavior

### Issue 4

Title:
- `DownloadStaffCSV: Enable Download Staff CSV button on Staff tab`

Purpose:
- Make the existing disabled frontend button actually trigger the CSV download.

Changes:
- Replace the disabled `Download Staff CSV` button in `StaffTabComponent`
- Add a click handler that opens:
  - `/api/csv/coursestaff?courseId=${courseId}`
- Leave `Upload CSV Roster` disabled

Why this is a separate issue:
- One frontend behavior concern
- User-visible and easy to validate

Acceptance criteria:
- `Download Staff CSV` is enabled
- Clicking it calls `window.open(..., "_blank")`
- Upload button remains disabled

Branch:
- `pm-downloadstaffcsv-button`

Commit / PR title:
- `PM - DownloadStaffCSV: enable staff tab download button`

Dokku deployment:
- Required

Validation:
- Visit Staff tab
- Click button
- Confirm browser opens download URL in a new tab

### Issue 5

Title:
- `DownloadStaffCSV: Document staff CSV format and add help link`

Purpose:
- Add user-facing documentation and a help shortcut for the staff CSV format.

Changes:
- Add a `staff-information` section to `HelpCsvPage`
- Add a sample staff CSV fixture
- Add a help/info icon near the download button
- Link the icon to `/help/csv#staff-information`
- Add frontend tests for help-page rendering and anchor navigation

Why this is a separate issue:
- Documentation/help UX is one concern
- Keeps button behavior PR narrow

Acceptance criteria:
- Help page includes an anchor with `id="staff-information"`
- Staff sample CSV matches backend export field order exactly
- Clicking the help icon opens `/help/csv#staff-information`
- Help-page tests cover the new section

Branch:
- `pm-downloadstaffcsv-help-docs`

Commit / PR title:
- `PM - DownloadStaffCSV: add staff CSV help docs and help link`

Dokku deployment:
- Required

Validation:
- Open help icon from Staff tab
- Confirm navigation to the staff section
- Confirm section text and example CSV are visible

## Merge Order

Recommended merge order:
1. DTO/service
2. Endpoint
3. Endpoint tests
4. Button
5. Help docs/help link

Why this order:
- Backend internals first
- Public API second
- Tests next
- Frontend behavior after endpoint exists
- Docs/help after final behavior is stable

## Branch and PR Workflow

For each issue:
1. Start from an up-to-date `main`
2. Create a new branch for exactly one issue
3. Do not stack on your other unmerged issue branch
4. Keep each PR to one concern
5. Put `PM - DownloadStaffCSV:` at the front of both commit messages and PR titles

Working rule:
- Work on exactly one issue at a time
- Finish that issue on its own branch before starting the next one
- After opening the PR for that issue, go back to updated `main` and create a fresh branch for the next issue

Do not do this:
- do not combine two sub-issues into one branch
- do not start issue 2 from issue 1's branch unless issue 1 is already merged and `main` has been updated
- do not reuse a branch from a different unrelated issue

Branch hygiene reminders from `pr_checklist.md`:
- one concern per PR
- one branch per PR
- start from updated `main`
- keep PRs small
- do not mix formatting-only edits with feature work

## Dokku Deployment Plan

Because your other dev deployment is tied to another unmerged issue, create a fresh Dokku app for each user-visible branch for this epic.

Use a dedicated Dokku deployment per PR branch when the change is moderate or visible in UI.

Recommended branches that should have deployments:
- Issue 2 if you want endpoint validation in-browser
- Issue 4
- Issue 5

When I should ask you to create a new Dokku dev deployment:
- Issue 2:
  only if you want live endpoint validation beyond local/backend tests
- Issue 4:
  yes, because the Staff tab button behavior is user-visible
- Issue 5:
  yes, because the help icon and help-page documentation are user-visible

How I should communicate that to you:
- I should explicitly tell you before that issue's PR is finalized:
  `This issue needs a fresh Dokku dev deployment. Please create a new Dokku app for branch <branch-name>.`

Default assumption:
- No Dokku deployment needed for Issue 1
- No Dokku deployment needed for Issue 3
- Dokku deployment likely needed for Issues 4 and 5
- Issue 2 is optional depending on how much live verification you want

Track these in this file while you work:
- Dokku app name
- deployment URL
- whether GitHub app setup was needed
- any extra testing accounts / admin access needed

Suggested tracking table:

| Issue | Branch | Dokku App | Deployment URL | PR URL | Status |
|---|---|---|---|---|---|
| 1 | `pm-downloadstaffcsv-dto-service` |  |  |  |  |
| 2 | `pm-downloadstaffcsv-endpoint` |  |  |  |  |
| 3 | `pm-downloadstaffcsv-endpoint-tests` |  |  |  |  |
| 4 | `pm-downloadstaffcsv-button` |  |  |  |  |
| 5 | `pm-downloadstaffcsv-help-docs` |  |  |  |  |

Dokku reminders from local docs:
- use a dedicated app per branch
- redeploy after pushing updates
- include deployment link in PR description
- if GitHub app settings are needed, follow:
  - `docs/dokku.md`
  - `docs/github-app-setup-dokku.md`

## PR Description Checklist

Each PR should include:
- a first sentence describing what changed for the user/developer
- `Closes #<issue-number>`
- testing steps
- deployment link if deployed
- screenshots for frontend/help-page changes
- Swagger or endpoint evidence for backend work where useful

Recommended PR title format:
- `PM - DownloadStaffCSV: <single concern>`

Recommended commit message format:
- `PM - DownloadStaffCSV: <single concern>`

Examples:
- `PM - DownloadStaffCSV: add CourseStaff CSV DTO and service`
- `PM - DownloadStaffCSV: add course staff CSV endpoint`
- `PM - DownloadStaffCSV: add tests for course staff CSV endpoint`
- `PM - DownloadStaffCSV: enable staff tab download button`
- `PM - DownloadStaffCSV: add staff CSV help docs and help link`

## Validation Checklist

### Backend
- Endpoint returns `404` for bad course id
- Endpoint returns downloadable CSV for valid course
- Filename is correct
- Header/content type is correct

### Frontend
- Staff tab download button is enabled
- Clicking the button calls the endpoint in a new tab
- Upload button remains disabled

### Help Page
- Staff section exists
- Anchor scroll works
- Sample CSV matches actual export format
- Help icon opens the right anchor URL

## Notes / Blockers

Use this section while working:
- blocker:
- merge dependency:
- dokku issue:
- reviewer note:
- follow-up issue:

## Current Progress Snapshot

Last updated: after finishing Issue `#28`

### Epic status
- Epic issue: `#16 DownloadStaffCSV`
- Sub-issues created and assigned to `parm2006`:
  - `#27` DTO/service
  - `#28` backend GET endpoint
  - `#29` controller tests
  - `#30` frontend Staff tab button
  - `#31` help docs/help link

### Finished issues

#### Issue #27
- Title:
  - `DownloadStaffCSV: Create DTO/service for CourseStaff CSV downloads`
- Branch:
  - `pm-downloadstaffcsv-dto-service`
- Commit:
  - `PM - DownloadStaffCSV: add CourseStaff CSV DTO and service`
- PR:
  - `#32`
  - <https://github.com/ucsb-cs156-s26/proj-frontiers-s26-09/pull/32>
- Dokku app:
  - `frontiers-27-parm2006`
- Dokku URL:
  - <https://frontiers-27-parm2006.dokku-09.cs.ucsb.edu>
- Status:
  - implemented
  - pushed
  - draft PR created
- Main files added:
  - `src/main/java/edu/ucsb/cs156/frontiers/models/CourseStaffDTO.java`
  - `src/main/java/edu/ucsb/cs156/frontiers/services/CourseStaffDTOService.java`
  - `src/test/java/edu/ucsb/cs156/frontiers/services/CourseStaffDTOServiceTests.java`
- Test used:
  - `mvn -Dtest=CourseStaffDTOServiceTests,RosterStudentDTOServiceTests test`

#### Issue #28
- Title:
  - `DownloadStaffCSV: Add backend endpoint for downloading course staff as CSV`
- Branch:
  - `PM-DownLoadStaffCSV-GET`
- Base branch:
  - `pm-downloadstaffcsv-dto-service`
- Commit:
  - `PM - DownloadStaffCSV: add course staff CSV endpoint`
- PR:
  - `#33`
  - <https://github.com/ucsb-cs156-s26/proj-frontiers-s26-09/pull/33>
- Dokku app:
  - `frontiers-28-parm2006`
- Dokku URL:
  - <https://frontiers-28-parm2006.dokku-09.cs.ucsb.edu>
- Status:
  - implemented
  - pushed
  - draft PR created
  - user said PR is finished
- Main files changed:
  - `src/main/java/edu/ucsb/cs156/frontiers/controllers/CSVDownloadsController.java`
  - `src/test/java/edu/ucsb/cs156/frontiers/controllers/CSVDownloadsControllerTests.java`
- Endpoint added:
  - `GET /api/csv/coursestaff?courseId=<courseId>`
- Direct deployed endpoint format:
  - `https://frontiers-28-parm2006.dokku-09.cs.ucsb.edu/api/csv/coursestaff?courseId=<courseId>`
- Direct Swagger link used:
  - <https://frontiers-28-parm2006.dokku-09.cs.ucsb.edu/swagger-ui/index.html#/CSV%20Downloads/csvForCourseStaff>
- Test used:
  - `mvn -Dtest=CSVDownloadsControllerTests test`
- Manual verification reminder:
  - create a course
  - add at least one staff member on the Staff tab
  - visit the endpoint URL directly while logged in

### Remaining issues
- `#29` Add controller tests for course staff CSV download
  - Note: this is now partially overlapped because Issue `#28` already added controller tests while implementing the endpoint.
  - Before starting `#29`, check whether it should be repurposed, reduced, or closed as already covered.
- `#30` Enable Download Staff CSV button on Staff tab
- `#31` Document staff CSV format and add help link

## Important Workflow Rules To Remember

These came from the user and should be reused in later CSV issues.

### Branching strategy
- First issue branch was created from `main`
- After that, new CSV issue branches should branch off the previous finished CSV branch, not `main`
- Reason:
  - the CSV issues depend on each other
  - PRs may not merge before the next issue starts

Current branch chain:
- `pm-downloadstaffcsv-dto-service` -> based on `main`
- `PM-DownLoadStaffCSV-GET` -> based on `pm-downloadstaffcsv-dto-service`
- next CSV issue branch should be based on `PM-DownLoadStaffCSV-GET` unless something changes

### PR base branch strategy
- Dependent PRs should target the previous CSV branch, not `main`
- Example:
  - `#27` PR -> base `main`
  - `#28` PR -> base `pm-downloadstaffcsv-dto-service`
- Reason:
  - keeps diffs small and focused
- Important GitHub behavior note:
  - `Closes #28` may not auto-close properly while the PR base is another feature branch
  - once retargeted to `main`, auto-close behavior should work normally

### Naming conventions
- Issue titles should start with `DownloadStaffCSV:`
- Commit messages should start with `PM - DownloadStaffCSV:`
- PR titles should start with `PM - DownloadStaffCSV:`

### Dokku / deployment rules
- User wants a fresh Dokku deployment for every CSV issue
- We should explicitly ask for or note the Dokku app name per branch
- PRs should include:
  - Dev deployment link
  - direct Swagger endpoint links when a PR adds endpoints
- For backend PRs with no UI:
  - Storybook is not needed
  - screenshots are usually not needed unless the user wants Swagger screenshots

### Swagger link rule
- When a PR adds an endpoint, include the direct Swagger operation link, not just the Swagger home page
- If a PR adds multiple endpoints, list multiple direct Swagger links
- If a PR adds no controller endpoint, say Swagger endpoint link is not applicable

### Validation/tooling notes
- For backend issues, targeted Maven tests were used successfully
- For frontend issues, user asked to use repo Node tooling like `npx` / npm scripts when applicable
- Avoid broad frontend format commands when possible because they can touch unrelated files

## Likely Next Step
- Start from branch `PM-DownLoadStaffCSV-GET`
- Reassess Issue `#29` because its intended work may already be covered by the tests added in `#28`
- If `#29` is still needed, define a narrower remaining scope before coding
- Otherwise move to `#30` for the frontend Staff tab download button
