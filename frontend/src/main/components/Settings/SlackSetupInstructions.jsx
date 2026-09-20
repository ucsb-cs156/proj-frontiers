import React from "react";
import { Accordion, Table } from "react-bootstrap";

// The content here should be kept in sync with the section
// "Setting up the Slack app" in docs/slack.md
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
              add the scopes for the capabilities Frontiers uses:
              <Table
                bordered
                size="sm"
                className="mt-2"
                data-testid={`${testIdPrefix}-scopes`}
              >
                <thead>
                  <tr>
                    <th>Capability</th>
                    <th>Scopes</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>List members + emails</td>
                    <td>
                      <code>users:read</code>, <code>users:read.email</code>{" "}
                      (emails are not included without the second one)
                    </td>
                  </tr>
                  <tr>
                    <td>Create channels + invite users</td>
                    <td>
                      <code>channels:manage</code> (public channels; also covers{" "}
                      <code>conversations.create</code> and{" "}
                      <code>conversations.invite</code>), plus{" "}
                      <code>groups:write</code> if you ever need private
                      channels
                    </td>
                  </tr>
                  <tr>
                    <td>List channel memberships</td>
                    <td>
                      <code>channels:read</code> (and <code>groups:read</code>{" "}
                      for private channels)
                    </td>
                  </tr>
                  <tr>
                    <td>Send messages</td>
                    <td>
                      <code>chat:write</code>, and optionally{" "}
                      <code>chat:write.public</code> so the bot can post to
                      public channels it hasn&apos;t joined
                    </td>
                  </tr>
                  <tr>
                    <td>Read public channel history</td>
                    <td>
                      <code>channels:history</code>, plus{" "}
                      <code>channels:join</code> so the bot can join channels
                      programmatically (a bot must be a member of a channel to
                      read its history; <code>chat:write.public</code> covers
                      posting but not reading)
                    </td>
                  </tr>
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
          </ol>
        </Accordion.Body>
      </Accordion.Item>
    </Accordion>
  );
}
