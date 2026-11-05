# CLAUDE.md

Project-specific instructions for Claude Code when working in this repository.

## PR descriptions: "Test Plan" vs "Quality Checks"

These are two distinct sections. Do not conflate them, and do not label the
second one "Test Plan":

- **Test Plan** — a human-facing section describing how a person would
  manually exercise the new/changed behavior on a deployed instance of the
  app: what page/URL to go to, what to click, what input to enter, and what
  result to expect. Written for a human reviewer who wants to try the change
  themselves, with no knowledge of the implementation. Numbered steps or a
  checklist, not a description of automated tests.

- **Quality Checks** — the list of automated steps taken to ensure
  correctness: new/updated unit and integration tests, coverage percentages
  (Jacoco), mutation testing scores (Pitest/Stryker), lint/format checks,
  `mvn verify` results, etc. This is developer-facing QA record-keeping. Use
  this exact heading — not "Test Plan" — for this content.

Every PR description should include a **Test Plan** section with real manual
testing steps whenever the change has any user-visible or clickable surface.
The **Quality Checks** section is additive, not a substitute for it.

## Referencing issues and PRs in chat

When mentioning a GitHub issue or PR number in a chat reply, make it a
hyperlink to that issue or PR on GitHub, e.g.
`[#842](https://github.com/ucsb-cs156/proj-frontiers/issues/842)` or
`[#843](https://github.com/ucsb-cs156/proj-frontiers/pull/843)`, rather than a
bare number.
