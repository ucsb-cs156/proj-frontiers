import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { beforeEach, describe, expect, test, vi } from "vitest";

import ApiKeysTabComponent from "main/components/TabComponent/ApiKeysTabComponent";
import { apiKeysFixtures } from "fixtures/apiKeysFixtures";
import { API_KEYS_DOCS_URL, apiKeysQueryKey } from "main/utils/apiKeyUtils";
import * as useBackendModule from "main/utils/useBackend";

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const useBackendSpy = vi.spyOn(useBackendModule, "useBackend");
const testId = "InstructorCourseShowPage";
const tableId = `${testId}-api-keys-table`;
const formId = "ApiKeyCreateForm";

const keysUrl = "/api/courses/key";
const keysGetHistory = () =>
  axiosMock.history.get.filter((request) => request.url === keysUrl);

const renderTab = () =>
  render(
    <QueryClientProvider client={queryClient}>
      <ApiKeysTabComponent courseId={7} testIdPrefix={testId} />
    </QueryClientProvider>,
  );

describe("ApiKeysTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    vi.clearAllMocks();
  });

  test("renders the explanation, the create button and the table from the backend", async () => {
    axiosMock.onGet(keysUrl).reply(200, apiKeysFixtures.severalKeys);

    renderTab();

    expect(
      screen.getByTestId(`${testId}-api-keys-tab-component`),
    ).toBeInTheDocument();
    // before the backend answers there are no rows
    expect(screen.queryByTestId(`${tableId}-row-0`)).not.toBeInTheDocument();
    expect(
      screen.getByText("API Keys", { selector: ".card-header" }),
    ).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-api-keys-text`)).toHaveTextContent(
      "API keys let a script, such as a Gradescope autograder or a GitHub Action, call a " +
        "few Frontiers endpoints for this course without logging in. Each key works only " +
        "for this course, expires after 90 days or 6 months, and can be revoked at any " +
        "time. A key is shown once, when it is created; Frontiers does not store it. " +
        'Turning off the "Enable Api Keys" course option disables every key of the course. ' +
        "See the API key documentation for the endpoints that accept a key and examples.",
    );
    const docsLink = screen.getByTestId(`${testId}-api-keys-docs-link`);
    expect(docsLink).toHaveTextContent("the API key documentation");
    expect(docsLink).toHaveAttribute("href", API_KEYS_DOCS_URL);
    expect(docsLink).toHaveAttribute("target", "_blank");
    expect(docsLink).toHaveAttribute("rel", "noopener noreferrer");
    expect(
      screen.getByTestId(`${testId}-create-api-key-button`),
    ).toHaveTextContent("Create API Key");
    expect(screen.getByTestId(tableId)).toBeInTheDocument();

    expect(
      await screen.findByTestId(`${tableId}-cell-row-0-col-label`),
    ).toHaveTextContent("jpa02 autograder F26");
    expect(
      screen.getByTestId(`${tableId}-cell-row-3-col-status`),
    ).toHaveTextContent("REVOKED");
    expect(
      screen.getByTestId(`${tableId}-cell-row-0-col-Revoke-button`),
    ).toBeInTheDocument();

    expect(keysGetHistory().length).toBe(1);
    expect(keysGetHistory()[0].params).toEqual({ courseId: 7 });
    // a failure to load the keys is shown by an empty table, not a toast
    expect(useBackendSpy).toHaveBeenCalledWith(
      [apiKeysQueryKey(7)],
      { method: "GET", url: "/api/courses/key", params: { courseId: 7 } },
      [],
      true,
    );
    expect(
      screen.queryByText("Create API Key", { selector: ".modal-title" }),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("Your new API key")).not.toBeInTheDocument();
  });

  test("creating a key with a label posts it, shows the key once, and refreshes the list", async () => {
    axiosMock.onGet(keysUrl).reply(200, []);
    axiosMock.onPost(keysUrl).reply(200, apiKeysFixtures.issuedKey);

    renderTab();
    await waitFor(() => expect(keysGetHistory().length).toBe(1));

    fireEvent.click(screen.getByTestId(`${testId}-create-api-key-button`));

    expect(
      await screen.findByText("Create API Key", { selector: ".modal-title" }),
    ).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-create-api-key-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    fireEvent.change(screen.getByTestId(`${formId}-label`), {
      target: { value: "jpa02 autograder F26" },
    });
    fireEvent.click(screen.getByTestId(`${formId}-submit`));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].url).toBe(keysUrl);
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 7,
      choice: "DAYS_90",
      label: "jpa02 autograder F26",
    });

    expect(await screen.findByText("Your new API key")).toBeInTheDocument();
    expect(screen.getByTestId("NewApiKeyModal-key")).toHaveTextContent(
      "u1Zc6N0T0dG9xWcY4h2JbQ",
    );
    expect(screen.getByTestId("NewApiKeyModal-example")).toHaveTextContent(
      "courseId=7&email=cgaucho@ucsb.edu",
    );
    await waitFor(() =>
      expect(
        screen.queryByText("Create API Key", { selector: ".modal-title" }),
      ).not.toBeInTheDocument(),
    );
    await waitFor(() => expect(keysGetHistory().length).toBe(2));

    fireEvent.click(screen.getByTestId("NewApiKeyModal-close"));
    await waitFor(() =>
      expect(screen.queryByText("Your new API key")).not.toBeInTheDocument(),
    );
  });

  test("creating a key without a label leaves the label parameter out", async () => {
    axiosMock.onGet(keysUrl).reply(200, []);
    axiosMock.onPost(keysUrl).reply(200, apiKeysFixtures.issuedKeyWithoutLabel);

    renderTab();

    fireEvent.click(screen.getByTestId(`${testId}-create-api-key-button`));
    fireEvent.click(await screen.findByTestId(`${formId}-choice-MONTHS_6`));
    fireEvent.click(screen.getByTestId(`${formId}-submit`));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 7,
      choice: "MONTHS_6",
    });
    expect(await screen.findByText("Your new API key")).toBeInTheDocument();
    expect(screen.getByTestId("NewApiKeyModal-details").textContent).toMatch(
      /^Expires: /,
    );
  });

  test("revoking a key from the table refreshes the list", async () => {
    axiosMock.onGet(keysUrl).reply(200, apiKeysFixtures.severalKeys);
    axiosMock
      .onDelete(keysUrl)
      .reply(200, { message: "API key with id 7 revoked" });

    renderTab();

    fireEvent.click(
      await screen.findByTestId(`${tableId}-cell-row-0-col-Revoke-button`),
    );
    fireEvent.click(await screen.findByText("Yes, I'd like to do this"));

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].params).toEqual({ courseId: 7, id: 7 });
    // the table's mutation invalidates the tab's query, so the list is fetched again
    await waitFor(() => expect(keysGetHistory().length).toBe(2));
  });

  test("the create modal can be closed without creating anything", async () => {
    axiosMock.onGet(keysUrl).reply(200, []);

    renderTab();

    fireEvent.click(screen.getByTestId(`${testId}-create-api-key-button`));
    expect(
      await screen.findByText("Create API Key", { selector: ".modal-title" }),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText("Close"));

    await waitFor(() =>
      expect(
        screen.queryByText("Create API Key", { selector: ".modal-title" }),
      ).not.toBeInTheDocument(),
    );
    expect(axiosMock.history.post.length).toBe(0);
  });
});
