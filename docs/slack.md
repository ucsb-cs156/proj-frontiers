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
   capabilities Frontiers uses:

   | Capability | Scopes |
   |------------|--------|
   | List members + emails | `users:read`, `users:read.email` (emails are not included without the second one) |
   | Create channels + invite users | `channels:manage` (public channels; also covers `conversations.create` and `conversations.invite`), plus `groups:write` if you ever need private channels |
   | List channel memberships | `channels:read` (and `groups:read` for private channels) |
   | Send messages | `chat:write`, and optionally `chat:write.public` so the bot can post to public channels it hasn't joined |
   | Read public channel history | `channels:history`, plus `channels:join` so the bot can join channels programmatically (a bot must be a member of a channel to read its history; `chat:write.public` covers posting but not reading) |

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
| `GET /api/courses/slack/info?courseId=...` (masked token, workspace id and name) | `SlackController` |
| `POST /api/courses/slack/token` (verify via `auth.test`, then encrypt and store) | `SlackController` |
| Call to Slack `auth.test` | `SlackService.authTest(...)` |
| In-app copy of "Setting up the Slack app" (keep in sync with this file) | `SlackSetupInstructions.jsx` |
| Settings card | `SlackCourseSettings.jsx`, `SlackTokenForm.jsx`, shown by `SettingsTabComponent.jsx` when the `SLACK_INTEGRATION` course option is enabled |
