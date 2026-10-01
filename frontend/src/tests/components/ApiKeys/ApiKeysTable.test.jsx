import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { toast } from "react-toastify";

import ApiKeysTable from "main/components/ApiKeys/ApiKeysTable";
import { describeKey } from "main/utils/apiKeyUtils";
import { apiKeysFixtures } from "fixtures/apiKeysFixtures";
import { formatTime } from "main/utils/dateUtils";

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const testId = "InstructorCourseShowPage-api-keys-table";

const renderTable = (props = {}) =>
  render(
    <QueryClientProvider client={queryClient}>
      <ApiKeysTable
        apiKeys={apiKeysFixtures.severalKeys}
        courseId={7}
        testIdPrefix={testId}
        {...props}
      />
    </QueryClientProvider>,
  );

describe("ApiKeysTable tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    vi.resetAllMocks();
    // Only Date is faked, so that waitFor still works. Key 6 of the fixture
    // expires on 2026-10-04 09:00 -07:00, i.e. in a little under 3 days.
    vi.useFakeTimers({ toFake: ["Date"] });
    vi.setSystemTime(new Date("2026-10-01T12:00:00-07:00"));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  test("describeKey uses the suffix, and the label when there is one", () => {
    expect(describeKey(apiKeysFixtures.severalKeys[0])).toBe(
      "…4h2JbQ (jpa02 autograder F26)",
    );
    expect(describeKey(apiKeysFixtures.severalKeys[2])).toBe("…aB3dEf");
    expect(describeKey({ keySuffix: "abcdef", label: "" })).toBe("…abcdef");
  });

  test("renders the headers and one row per key", () => {
    renderTable();

    const headers = [
      ["label", "Label"],
      ["keySuffix", "Key"],
      ["createdByEmail", "Created by"],
      ["createdAt", "Created"],
      ["expiresAt", "Expires"],
      ["lastUsedAt", "Last used"],
      ["usageCount", "Uses"],
      ["status", "Status"],
      ["Revoke", "Revoke"],
    ];
    for (const [id, text] of headers) {
      expect(screen.getByTestId(`${testId}-header-${id}`)).toHaveTextContent(
        text,
      );
    }

    const [active, expiringSoon, expired, revoked] =
      apiKeysFixtures.severalKeys;

    // row 0: an active key with a label, far from expiry
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-label`),
    ).toHaveTextContent("jpa02 autograder F26");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-keySuffix`),
    ).toHaveTextContent("…4h2JbQ");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-createdByEmail`),
    ).toHaveTextContent("phtcon@ucsb.edu");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-createdAt`),
    ).toHaveTextContent(formatTime(active.createdAt));
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-expiresAt`),
    ).toHaveTextContent(formatTime(active.expiresAt));
    expect(
      screen.queryByTestId(`${testId}-row-0-expiry-warning`),
    ).not.toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-lastUsedAt`),
    ).toHaveTextContent(formatTime(active.lastUsedAt));
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-usageCount`),
    ).toHaveTextContent("143");
    const activeBadge = screen.getByTestId(
      `${testId}-cell-row-0-col-status`,
    ).firstChild;
    expect(activeBadge).toHaveTextContent("ACTIVE");
    expect(activeBadge).toHaveClass("badge", "bg-success");
    const revokeButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Revoke-button`,
    );
    expect(revokeButton).toHaveTextContent("Revoke");
    expect(revokeButton).toHaveClass("btn-danger");

    // row 1: active, never used, expiring within the warning window
    expect(
      screen.getByTestId(`${testId}-cell-row-1-col-lastUsedAt`),
    ).toHaveTextContent("never");
    expect(
      screen.getByTestId(`${testId}-cell-row-1-col-expiresAt`),
    ).toHaveTextContent(formatTime(expiringSoon.expiresAt));
    const warning = screen.getByTestId(`${testId}-row-1-expiry-warning`);
    expect(warning).toHaveTextContent("expires in 3 days");
    expect(warning).toHaveClass("badge", "bg-warning", "text-dark", "ms-2");
    expect(
      screen.getByTestId(`${testId}-cell-row-1-col-Revoke-button`),
    ).toBeInTheDocument();

    // row 2: expired, no label: no warning, no revoke button
    expect(
      screen.getByTestId(`${testId}-cell-row-2-col-label`),
    ).toHaveTextContent("");
    expect(
      screen.getByTestId(`${testId}-cell-row-2-col-expiresAt`),
    ).toHaveTextContent(formatTime(expired.expiresAt));
    expect(
      screen.queryByTestId(`${testId}-row-2-expiry-warning`),
    ).not.toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-cell-row-2-col-status`).firstChild,
    ).toHaveClass("bg-secondary");
    expect(
      screen.getByTestId(`${testId}-cell-row-2-col-status`),
    ).toHaveTextContent("EXPIRED");
    expect(
      screen.queryByTestId(`${testId}-cell-row-2-col-Revoke-button`),
    ).not.toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-cell-row-2-col-Revoke`),
    ).toBeEmptyDOMElement();

    // row 3: revoked (and not yet expired): no warning, no revoke button
    expect(
      screen.getByTestId(`${testId}-cell-row-3-col-expiresAt`),
    ).toHaveTextContent(formatTime(revoked.expiresAt));
    expect(
      screen.queryByTestId(`${testId}-row-3-expiry-warning`),
    ).not.toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-cell-row-3-col-status`).firstChild,
    ).toHaveClass("bg-danger");
    expect(
      screen.getByTestId(`${testId}-cell-row-3-col-status`),
    ).toHaveTextContent("REVOKED");
    expect(
      screen.queryByTestId(`${testId}-cell-row-3-col-Revoke-button`),
    ).not.toBeInTheDocument();

    expect(screen.queryByTestId(`${testId}-row-4`)).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("ConfirmationModal-base"),
    ).not.toBeInTheDocument();
  });

  test("the expiry warning appears at 14 days but not at 15", () => {
    // now is 2026-10-01T12:00:00-07:00 = 2026-10-01T19:00:00Z
    const base = { ...apiKeysFixtures.severalKeys[0], lastUsedAt: null };
    renderTable({
      apiKeys: [
        { ...base, id: 1, expiresAt: "2026-10-15T19:00:00Z" },
        { ...base, id: 2, expiresAt: "2026-10-16T19:00:00Z" },
        { ...base, id: 3, expiresAt: "2026-10-01T20:00:00Z" },
      ],
    });

    expect(
      screen.getByTestId(`${testId}-row-0-expiry-warning`),
    ).toHaveTextContent("expires in 14 days");
    expect(
      screen.queryByTestId(`${testId}-row-1-expiry-warning`),
    ).not.toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-row-2-expiry-warning`),
    ).toHaveTextContent("expires in 1 days");
  });

  test("renders an empty table", () => {
    renderTable({ apiKeys: [] });
    expect(screen.getByTestId(`${testId}-header-label`)).toBeInTheDocument();
    expect(
      screen.queryByTestId(`${testId}-cell-row-0-col-label`),
    ).not.toBeInTheDocument();
  });

  test("uses the default testIdPrefix", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <ApiKeysTable apiKeys={apiKeysFixtures.oneKey} courseId={7} />
      </QueryClientProvider>,
    );
    expect(screen.getByTestId("ApiKeysTable")).toBeInTheDocument();
    expect(
      screen.getByTestId("ApiKeysTable-cell-row-0-col-Revoke-button"),
    ).toBeInTheDocument();
  });

  test("revoke asks for confirmation, then sends DELETE for that key and the course", async () => {
    axiosMock
      .onDelete("/api/courses/key")
      .reply(200, { message: "API key with id 7 revoked" });

    renderTable();

    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-0-col-Revoke-button`),
    );

    expect(
      await screen.findByTestId("ConfirmationModal-base"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-revoke-confirmation-message`),
    ).toHaveTextContent(
      "Are you sure you want to revoke the API key …4h2JbQ (jpa02 autograder F26)? " +
        "Anything using it will stop working immediately. This cannot be undone.",
    );

    fireEvent.click(screen.getByText("Yes, I'd like to do this"));

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].url).toBe("/api/courses/key");
    expect(axiosMock.history.delete[0].params).toEqual({ courseId: 7, id: 7 });
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "API key …4h2JbQ (jpa02 autograder F26) revoked.",
      ),
    );
    await waitFor(() =>
      expect(
        screen.queryByTestId("ConfirmationModal-base"),
      ).not.toBeInTheDocument(),
    );
  });

  test("revoking a key without a label names it by its suffix only", async () => {
    axiosMock
      .onDelete("/api/courses/key")
      .reply(200, { message: "API key with id 9 revoked" });
    renderTable({
      apiKeys: [
        {
          ...apiKeysFixtures.severalKeys[0],
          id: 9,
          label: null,
          keySuffix: "noLabl",
        },
      ],
    });

    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-0-col-Revoke-button`),
    );
    expect(
      await screen.findByTestId(`${testId}-revoke-confirmation-message`),
    ).toHaveTextContent(
      "Are you sure you want to revoke the API key …noLabl? Anything using it will stop working immediately. This cannot be undone.",
    );

    fireEvent.click(screen.getByText("Yes, I'd like to do this"));

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].params).toEqual({ courseId: 7, id: 9 });
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith("API key …noLabl revoked."),
    );
  });

  test("backing out of the confirmation sends nothing", async () => {
    renderTable();

    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-1-col-Revoke-button`),
    );
    expect(
      await screen.findByTestId(`${testId}-revoke-confirmation-message`),
    ).toHaveTextContent("…Qz9xLm (team03 github action)");

    fireEvent.click(screen.getByText("No, take me back"));

    await waitFor(() =>
      expect(
        screen.queryByTestId("ConfirmationModal-base"),
      ).not.toBeInTheDocument(),
    );
    expect(axiosMock.history.delete.length).toBe(0);
    expect(toast).not.toHaveBeenCalled();
  });
});
