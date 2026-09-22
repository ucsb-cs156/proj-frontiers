import React from "react";
import { Accordion, Table } from "react-bootstrap";

// The content here should be kept in sync with the section
// "Setting up the Slack app" in docs/slack.md

// One scope per row
const slackScopes = [
  { capability: "List members", scope: "users:read" },
  { capability: "List members' emails", scope: "users:read.email" },
  {
    capability: "Create public channels and invite users to them",
    scope: "channels:manage",
  },
  {
    capability: "Create private channels and invite users to them",
    scope: "groups:write",
  },
  { capability: "List public channel memberships", scope: "channels:read" },
  { capability: "List private channel memberships", scope: "groups:read" },
  { capability: "Send messages", scope: "chat:write" },
  {
    capability: "Send messages to public channels the bot has not joined",
    scope: "chat:write.public",
  },
  { capability: "Read public channel history", scope: "channels:history" },
  {
    capability:
      "Join public channels (the bot must be a member of a channel to read its history)",
    scope: "channels:join",
  },
];
export default function SlackSetupInstructions({
  testIdPrefix = "SlackSetupInstructions",
}) {
  return (
    <Accordion className="mb-3" data-testid={`${testIdPrefix}-instructions`}>
      <Accordion.Item eventKey="instructions">
        <Accordion.Header>
          Instructions: Setting up the Slack app
        </Accordion.Header>
        <Accordion.Body>
          <ol>
            <li>
              Create an app at{" "}
              <a
                href="https://api.slack.com/apps"
                target="_blank"
                rel="noopener noreferrer"
              >
                https://api.slack.com/apps
              </a>{" "}
              (choose &quot;from scratch&quot;), tied to the target workspace.
            </li>
            <li>
              Under <strong>OAuth &amp; Permissions → Bot Token Scopes</strong>,
              add the scopes for the capabilities Frontiers uses (one scope per
              row):
              <Table
                bordered
                size="sm"
                className="mt-2"
                data-testid={`${testIdPrefix}-scopes`}
              >
                <thead>
                  <tr>
                    <th>Capability</th>
                    <th>Scope</th>
                  </tr>
                </thead>
                <tbody>
                  {slackScopes.map(({ capability, scope }) => (
                    <tr key={scope}>
                      <td>{capability}</td>
                      <td>
                        <code>{scope}</code>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </li>
            <li>
              Install the app to the workspace (the button is on the same page).
            </li>
            <li>
              Copy the <strong>Bot User OAuth Token</strong> (it starts with{" "}
              <code>xoxb-</code>). That is what goes into the Frontiers course
              settings.
            </li>
            <li>
              Allow the app to remove people from public channels. By default
              only Workspace Owners and Admins may, and then the section and
              team channel jobs cannot remove anyone (their log shows{" "}
              <code>restricted_action</code>). A Workspace Owner opens{" "}
              <strong>Workspace settings → Roles &amp; permissions</strong> (on
              older workspaces,{" "}
              <strong>Permissions → Channel Management</strong>) and sets{" "}
              <strong>
                People who can remove members from public channels
              </strong>{" "}
              to <strong>Everyone, except guests</strong>.
            </li>
          </ol>
        </Accordion.Body>
      </Accordion.Item>
    </Accordion>
  );
}
