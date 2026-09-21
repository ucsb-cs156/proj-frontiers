# Slack Integration

Frontiers can be connected to a Slack workspace on a per-course basis. The
connection is made by pasting a Slack **bot token** into the course's Settings
tab. A bot token is already tied to one specific Slack workspace, so the token
is the only thing that has to be entered: it is sufficient to identify where
API calls go.

## Setting up the Slack app

1. Create an app at <https://api.slack.com/apps> (choose "from scratch"), tied
   to the target workspace.
2. Under **OAuth & Permissions → Bot Token Scopes**, add the scopes for the
   capabilities Frontiers uses (one scope per row):

   | Capability | Scope |
   |------------|-------|
   | List members | `users:read` |
   | List members' emails | `users:read.email` |
   | Create public channels and invite users to them | `channels:manage` |
   | Create private channels and invite users to them | `groups:write` |
   | List public channel memberships | `channels:read` |
   | List private channel memberships | `groups:read` |
   | Send messages | `chat:write` |
   | Send messages to public channels the bot has not joined | `chat:write.public` |
   | Read public channel history | `channels:history` |
   | Join public channels (the bot must be a member of a channel to read its history) | `channels:join` |

3. Install the app to the workspace (the button is on the same page).
4. Copy the **Bot User OAuth Token** (it starts with `xoxb-`). That is what goes
   into the Frontiers course settings.

## Entering the token in Frontiers

1. Go to the course page and open the **Settings** tab.
2. Under **Course Options**, turn on **Slack Integration**. A
   **Slack Integration Settings** card appears. (The card has a collapsible
   **Instructions: Setting up the Slack app** section that repeats the steps
   above.)
3. Paste the bot token into the **Slack Bot Token** field and click
   **Verify and Save Token**.

As a sanity check, Frontiers calls Slack's
[`auth.test`](https://docs.slack.dev/reference/methods/auth.test) method with
the token before saving it:

* If the token is valid, it is saved, and the name and ID of the Slack
  workspace it belongs to are displayed. Check that this is the workspace you
  intended.
* If the token is invalid (or revoked, etc.), an error message is shown, and
  the token is **not** saved. Any previously saved token is left in place.

Once a token has been saved, only its first four and last four characters are
ever shown; the rest is masked.

## The Slack tab

Once **Slack Integration** is turned on *and* a token has been saved, a **Slack**
tab appears on the course page. (Turn the option off, or never save a token, and
the tab is not shown.) The information on it is fetched from Slack when the tab
is opened. The tab has:

* A link to the Slack workspace that the token belongs to, and an **Admin**
  link to the workspace's administration pages (the workspace URL followed by
  `/admin`), where members can be invited, deactivated, and so on. The Admin
  link is not shown for a token that was saved before Frontiers started
  recording the workspace URL; saving the token again makes it appear.
* **Active Slack users**: every person with an active account in the workspace
  (bots, deactivated accounts, and people who have been invited but have not
  signed in yet are left out). The **Course Role** column shows whether the
  email of the Slack user matches the instructor, a staff member, or a student
  on the roster of the course, or none of these. Click the column header to sort by it, which is a quick way to find
  people in the workspace who are not part of the course.
* **Roster students and staff not active in Slack**: staff and roster students
  whose email does not match an active Slack user. The
  **Slack Status** column shows whether they have been invited but have not
  signed in yet, have a deactivated account, or are not known to the workspace
  at all.

In both tables, the only roster students considered are those whose roster
status is `ROSTER` or `MANUAL`. Dropped students are ignored: a dropped student
who is still in the Slack workspace shows up with Course Role "None", and a
dropped student who is not in Slack is not listed as missing.

Matching is by email, ignoring case, and treating `@umail.ucsb.edu` and
`@ucsb.edu` as the same. This needs the `users:read` and `users:read.email`
scopes; without the second one Slack provides no emails, so nobody will match.
Someone who signed up for Slack with a different email than the one on the
roster shows up in both tables: as "None" in the first, and as "Not in Slack"
in the second.

Whether someone has a pending invitation is determined from the
`is_invited_user` flag that Slack puts on members returned by
[`users.list`](https://docs.slack.dev/reference/methods/users.list). Slack's
dedicated APIs for managing invitations are only available on Enterprise Grid.

## Slack channels for sections

When **Translate Sections** is enabled as well as **Slack Integration**, the
bottom of the Slack tab has a **Slack Section Channels** card. Open it and
click **Set Up Section Slack Channels** to launch the
`SetupSectionSlackChannels` job for the course. What the job does is logged,
and can be read on the **Jobs** tab:

1. **Creating Section Channels**: for each row on the **Sections** tab that has
   a **Slack Channel** name, a public channel with that name is created, unless
   it already exists. The name is lowercased, and a leading `#` is dropped.
   Several sections can share a channel by using the same name. A channel that
   exists but has been archived is skipped (unarchive it in Slack first), as is
   a channel that Slack refuses to create, for example because the name has
   characters that Slack does not allow; the log says why.
2. **Adding Students to Channel**: each roster student (roster status `ROSTER`
   or `MANUAL`) whose section has a channel is added to it, unless they are in
   it already. Only students who are added are logged. Students are matched to
   Slack users by email, so a student who does not have an active account in
   the workspace cannot be added; the log says how many there were, and the
   second table on the Slack tab says who they are. Run the job again once they
   have joined.
3. **Removing Channel Members Who Are Not In The Section**: everyone else is
   removed from each of those channels, and logged, except for the staff of the
   course, the instructor, bots (including the Frontiers bot itself), and
   members that are not users of the workspace. That includes students who have
   dropped, or moved to another section.

The job can be run as often as you like; it only makes the changes that are
still needed. A problem with one channel or one person is logged, and the job
carries on with the rest. As a safety measure, the job stops before changing
anything if Slack does not provide any emails (that is, if the
`users:read.email` scope is missing), since it would otherwise remove everybody
from the channels.

The job needs these scopes: `users:read`, `users:read.email`, `channels:read`,
`channels:manage` and `channels:join`. Whether the bot is *allowed* to remove
people from public channels also depends on the workspace's settings
(**Settings & permissions → Permissions → Channel Management** in the Slack
admin pages); if it is not, the log shows `restricted_action` for each person
it could not remove.

Slack limits how fast an app may make these calls. When Slack says to slow
down, the job waits for as long as Slack asks (at most a minute at a time) and
tries again, so with a large class the job can take a few minutes.

## Operational gotcha: adding scopes means a new token

If you add scopes to the Slack app later, you must **reinstall the app to the
workspace**, which issues a **new token**. Paste the new token into the same
field on the Settings tab; it replaces the old one. A call that fails because
of a scope that has not been granted is reported by Slack as `missing_scope`.

## How the token is protected

The bot token is treated as a secret, using the same infrastructure as Canvas
API tokens (see [README_Canvas_API_Keys.md](README_Canvas_API_Keys.md)):

* It is encrypted at rest (AES-GCM) in the `COURSE.SLACK_BOT_TOKEN` column,
  using the key in the `TOKEN_ENCRYPTION_KEY` environment variable.
  That variable must be set before a token can be saved; use
  `openssl rand -base64 32` to generate a value. If it is missing or malformed,
  saving fails closed: the token is **not** stored (never in plaintext), and the
  Slack Integration Settings card shows a message pointing at this variable.
* It is never logged, is excluded from JSON serialization and `toString()` of
  the `Course` entity, and is only returned to the frontend in masked form.
* The frontend sends it in the body of a `POST` request rather than in the
  query string, so that it does not end up in access logs.

## Things that are not needed (yet)

* **Client ID / Client secret / OAuth redirect flow**: only needed if the app
  were to be distributable to many workspaces with an "Add to Slack" button.
  For one known workspace, a pasted bot token is the standard simple approach.
* **Signing secret / Request URL**: only needed if Slack pushes events to
  Frontiers (Events API, slash commands, interactive buttons). Reading messages
  by polling `conversations.history` doesn't require it; receiving messages in
  real time does.

## Implementation notes

| Piece | Where |
|-------|-------|
| `GET /api/courses/slack/info?courseId=...` (masked token, workspace id, name and URL) | `SlackController` |
| `POST /api/courses/slack/token` (verify via `auth.test`, then encrypt and store) | `SlackController` |
| `GET /api/courses/slack/users?courseId=...` (active Slack users, with course role) | `SlackController` |
| `GET /api/courses/slack/missing?courseId=...` (staff and students not active in Slack) | `SlackController` |
| Call to Slack `auth.test` | `SlackService.authTest(...)` |
| Calls to Slack `users.list` (follows pagination) | `SlackService.listUsers(...)` |
| `POST /api/courses/slack/sectionChannels?courseId=...` (launches the job) | `SlackController` |
| Job that sets up the section channels | `SetupSectionSlackChannelsJob` |
| Calls to Slack `conversations.list`, `.create`, `.join`, `.members`, `.invite`, `.kick` (with retry when rate limited) | `SlackService` |
| Slack Section Channels card | `SlackSectionChannelsCard.jsx`, shown by `SlackTabComponent.jsx` |
| Slack tab | `SlackTabComponent.jsx`, `SlackUsersTable.jsx`, `SlackMissingMembersTable.jsx`, shown by `InstructorCourseShowPage.jsx` |
| In-app copy of "Setting up the Slack app" (keep in sync with this file) | `SlackSetupInstructions.jsx` |
| Settings card | `SlackCourseSettings.jsx`, `SlackTokenForm.jsx`, shown by `SettingsTabComponent.jsx` when the `SLACK_INTEGRATION` course option is enabled |
