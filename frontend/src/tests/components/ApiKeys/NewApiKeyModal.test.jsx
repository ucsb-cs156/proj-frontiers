import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { toast } from "react-toastify";

import NewApiKeyModal from "main/components/ApiKeys/NewApiKeyModal";
import { apiKeysFixtures } from "fixtures/apiKeysFixtures";
import { formatTime } from "main/utils/dateUtils";
import {
  API_KEYS_DOCS_URL,
  studentInfoCurlExample,
} from "main/utils/apiKeyUtils";

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

const testId = "NewApiKeyModal";

describe("NewApiKeyModal tests", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  test("renders nothing when there is no key to show", () => {
    const { container } = render(
      <NewApiKeyModal issuedKey={null} courseId={7} onClose={vi.fn()} />,
    );
    expect(container).toBeEmptyDOMElement();
    expect(screen.queryByText("Your new API key")).not.toBeInTheDocument();
  });

  test("shows the key once, with its label, expiry and an example request", () => {
    const issuedKey = apiKeysFixtures.issuedKey;
    render(
      <NewApiKeyModal issuedKey={issuedKey} courseId={7} onClose={vi.fn()} />,
    );

    expect(screen.getByText("Your new API key")).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-modal`)).toHaveClass(
      "modal-dialog modal-lg modal-dialog-centered",
    );
    expect(screen.getByTestId(`${testId}-warning`)).toHaveTextContent(
      "Copy this key now. Frontiers does not store it, so it cannot be shown again. " +
        "If you lose it, revoke it and create a new one.",
    );
    expect(screen.getByTestId(`${testId}-warning`)).toHaveClass(
      "alert-warning",
    );
    expect(screen.getByTestId(`${testId}-key`)).toHaveTextContent(
      "u1Zc6N0T0dG9xWcY4h2JbQ",
    );
    expect(screen.getByTestId(`${testId}-copy`)).toHaveTextContent("Copy");
    expect(screen.getByTestId(`${testId}-details`)).toHaveTextContent(
      `Label: jpa02 autograder F26. Expires: ${formatTime(issuedKey.expiresAt)}.`,
    );
    expect(screen.getByTestId(`${testId}-example-intro`).textContent).toBe(
      "Example: look up a student the way a Gradescope autograder would (see " +
        "the API key documentation for the other endpoints):",
    );
    const docsLink = screen.getByTestId(`${testId}-docs-link`);
    expect(docsLink).toHaveTextContent("the API key documentation");
    expect(docsLink).toHaveAttribute("href", API_KEYS_DOCS_URL);
    expect(docsLink).toHaveAttribute("target", "_blank");
    expect(docsLink).toHaveAttribute("rel", "noopener noreferrer");
    expect(screen.getByTestId(`${testId}-example`)).toHaveTextContent(
      studentInfoCurlExample(7, issuedKey.key),
      { normalizeWhitespace: false },
    );
    expect(screen.getByTestId(`${testId}-example`).textContent).toContain(
      "courseId=7&email=cgaucho@ucsb.edu",
    );
    expect(screen.getByTestId(`${testId}-close`)).toHaveTextContent(
      "I have copied the key",
    );
  });

  test("leaves the label out of the details when the key has none", () => {
    const issuedKey = apiKeysFixtures.issuedKeyWithoutLabel;
    render(
      <NewApiKeyModal issuedKey={issuedKey} courseId={7} onClose={vi.fn()} />,
    );
    expect(screen.getByTestId(`${testId}-details`).textContent).toBe(
      `Expires: ${formatTime(issuedKey.expiresAt)}.`,
    );
    expect(screen.getByTestId(`${testId}-key`)).toHaveTextContent(
      issuedKey.key,
    );
  });

  test("the copy button puts the key on the clipboard", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, { clipboard: { writeText } });
    render(
      <NewApiKeyModal
        issuedKey={apiKeysFixtures.issuedKey}
        courseId={7}
        onClose={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByTestId(`${testId}-copy`));

    await waitFor(() =>
      expect(writeText).toHaveBeenCalledWith("u1Zc6N0T0dG9xWcY4h2JbQ"),
    );
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith("API key copied to clipboard."),
    );
  });

  test("the close button and the header close both call onClose", () => {
    const onClose = vi.fn();
    render(
      <NewApiKeyModal
        issuedKey={apiKeysFixtures.issuedKey}
        courseId={7}
        onClose={onClose}
      />,
    );

    fireEvent.click(screen.getByTestId(`${testId}-close`));
    expect(onClose).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByLabelText("Close"));
    expect(onClose).toHaveBeenCalledTimes(2);
  });

  test("uses a custom testIdPrefix", () => {
    render(
      <NewApiKeyModal
        issuedKey={apiKeysFixtures.issuedKey}
        courseId={7}
        onClose={vi.fn()}
        testIdPrefix="Custom"
      />,
    );
    expect(screen.getByTestId("Custom-key")).toBeInTheDocument();
    expect(screen.getByTestId("Custom-close")).toBeInTheDocument();
  });
});
