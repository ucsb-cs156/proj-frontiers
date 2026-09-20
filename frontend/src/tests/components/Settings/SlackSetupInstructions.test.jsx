import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { expect } from "vitest";
import SlackSetupInstructions from "main/components/Settings/SlackSetupInstructions";

// Collapses runs of whitespace so that expected text can be compared with textContent
const normalize = (text) => text.replace(/\s+/g, " ").trim();

describe("SlackSetupInstructions tests", () => {
  test("is collapsed by default and expands when the header is clicked", async () => {
    render(<SlackSetupInstructions />);

    const accordion = screen.getByTestId("SlackSetupInstructions-instructions");
    expect(accordion).toHaveClass("accordion", "mb-3");

    const header = screen.getByRole("button", {
      name: "Instructions: Setting up the Slack app",
    });
    expect(header).toHaveAttribute("aria-expanded", "false");
    expect(header).toHaveClass("collapsed");
    const collapse = accordion.querySelector(".accordion-collapse");
    expect(collapse).not.toHaveClass("show");

    fireEvent.click(header);

    await waitFor(() =>
      expect(header).toHaveAttribute("aria-expanded", "true"),
    );
    expect(header).not.toHaveClass("collapsed");
    await waitFor(() => expect(collapse).toHaveClass("show"));

    fireEvent.click(header);
    await waitFor(() =>
      expect(header).toHaveAttribute("aria-expanded", "false"),
    );
  });

  test("has the steps from docs/slack.md", async () => {
    render(<SlackSetupInstructions />);

    const accordion = screen.getByTestId("SlackSetupInstructions-instructions");
    const steps = accordion.querySelectorAll("ol > li");
    expect(steps.length).toBe(4);

    expect(normalize(steps[0].textContent)).toBe(
      'Create an app at https://api.slack.com/apps (choose "from scratch"), tied to the target workspace.',
    );
    expect(normalize(steps[1].textContent)).toMatch(
      /^Under OAuth & Permissions → Bot Token Scopes, add the scopes for the capabilities Frontiers uses:/,
    );
    expect(normalize(steps[2].textContent)).toBe(
      "Install the app to the workspace (the button is on the same page).",
    );
    expect(normalize(steps[3].textContent)).toBe(
      "Copy the Bot User OAuth Token (it starts with xoxb-). That is what goes into the Frontiers course settings.",
    );

    const link = within(steps[0]).getByRole("link", {
      name: "https://api.slack.com/apps",
    });
    expect(link).toHaveAttribute("href", "https://api.slack.com/apps");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });

  test("has the table of scopes from docs/slack.md", async () => {
    render(<SlackSetupInstructions testIdPrefix="Custom" />);

    expect(screen.getByTestId("Custom-instructions")).toBeInTheDocument();
    const table = screen.getByTestId("Custom-scopes");
    expect(table).toHaveClass("table", "table-bordered", "table-sm", "mt-2");

    const rows = Array.from(table.querySelectorAll("tr")).map((row) =>
      Array.from(row.children).map((cell) => normalize(cell.textContent)),
    );
    expect(rows).toEqual([
      ["Capability", "Scopes"],
      [
        "List members + emails",
        "users:read, users:read.email (emails are not included without the second one)",
      ],
      [
        "Create channels + invite users",
        "channels:manage (public channels; also covers conversations.create and conversations.invite), plus groups:write if you ever need private channels",
      ],
      [
        "List channel memberships",
        "channels:read (and groups:read for private channels)",
      ],
      [
        "Send messages",
        "chat:write, and optionally chat:write.public so the bot can post to public channels it hasn't joined",
      ],
      [
        "Read public channel history",
        "channels:history, plus channels:join so the bot can join channels programmatically (a bot must be a member of a channel to read its history; chat:write.public covers posting but not reading)",
      ],
    ]);

    const scopes = Array.from(table.querySelectorAll("code")).map(
      (code) => code.textContent,
    );
    expect(scopes).toEqual([
      "users:read",
      "users:read.email",
      "channels:manage",
      "conversations.create",
      "conversations.invite",
      "groups:write",
      "channels:read",
      "groups:read",
      "chat:write",
      "chat:write.public",
      "channels:history",
      "channels:join",
      "chat:write.public",
    ]);
  });
});
