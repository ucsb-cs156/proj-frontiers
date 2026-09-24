import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import userEvent from "@testing-library/user-event";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { beforeEach, describe, expect, test, vi } from "vitest";

import CourseEmailsCard from "main/components/Emails/CourseEmailsCard";
import { EMAIL_FORMATS } from "main/utils/courseEmailsUtils";

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

const axiosMock = new AxiosMockAdapter(axios);
const testId = "CourseEmailsCard";
const emailsUrl = "/api/courses/emails";
const emailsGetHistory = () =>
  axiosMock.history.get.filter((request) => request.url === emailsUrl);

const onePerLine = "a@ucsb.edu\r\nb@ucsb.edu\r\nc@ucsb.edu";
const commaSeparated = "a@ucsb.edu,b@ucsb.edu,c@ucsb.edu";

const renderCard = (props = {}) => {
  const queryClient = new QueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      <CourseEmailsCard courseId={7} type="STAFF" {...props} />
    </QueryClientProvider>,
  );
};

describe("CourseEmailsCard tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    mockToast.mockClear();
    axiosMock.onGet(emailsUrl).reply((config) => {
      return [
        200,
        config.params.format === "COMMA_SEPARATED"
          ? commaSeparated
          : onePerLine,
      ];
    });
  });

  test("renders a card, open by default, titled Emails", async () => {
    renderCard();

    const card = screen.getByTestId(`${testId}-card`);
    expect(card).toHaveClass("card", "mt-4");
    expect(card.querySelector(".accordion")).toHaveClass("accordion-flush");

    const header = screen.getByRole("button", { name: "Emails" });
    expect(header).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByTestId(`${testId}-emails`)).toBeVisible();

    fireEvent.click(header);
    await waitFor(() =>
      expect(header).toHaveAttribute("aria-expanded", "false"),
    );
    fireEvent.click(header);
    await waitFor(() =>
      expect(header).toHaveAttribute("aria-expanded", "true"),
    );
  });

  test("asks for the emails of the given type, one per line, without a team", async () => {
    renderCard({ type: "STUDENTS" });

    await waitFor(() => expect(emailsGetHistory().length).toBe(1));
    expect(emailsGetHistory()[0].params).toEqual({
      courseId: 7,
      type: "STUDENTS",
      format: "ONE_PER_LINE",
    });
    expect(emailsGetHistory()[0].params).not.toHaveProperty("team");
  });

  test("shows the emails in a read only, monospace, white on black text area", async () => {
    renderCard();

    const textarea = screen.getByTestId(`${testId}-emails`);
    expect(textarea.tagName).toBe("TEXTAREA");
    expect(textarea).toHaveAttribute("readonly");
    expect(textarea).toHaveAttribute("aria-label", "Emails");
    expect(textarea).toHaveStyle({
      fontFamily: "monospace",
      backgroundColor: "rgb(0, 0, 0)",
      color: "rgb(255, 255, 255)",
    });
    expect(textarea).toHaveValue("");

    await waitFor(() =>
      expect(textarea).toHaveValue("a@ucsb.edu\nb@ucsb.edu\nc@ucsb.edu"),
    );
  });

  test("the format dropdown offers One per line and Comma separated, One per line first", () => {
    renderCard();

    const select = screen.getByTestId(`${testId}-format`);
    expect(screen.getByLabelText("Format")).toBe(select);
    expect(select).toHaveValue("ONE_PER_LINE");
    const options = Array.from(select.querySelectorAll("option")).map((o) => [
      o.value,
      o.textContent,
    ]);
    expect(options).toEqual([
      ["ONE_PER_LINE", "One per line"],
      ["COMMA_SEPARATED", "Comma separated"],
    ]);
    expect(EMAIL_FORMATS.map((f) => f.value)).toEqual([
      "ONE_PER_LINE",
      "COMMA_SEPARATED",
    ]);
  });

  test("choosing Comma separated refetches in that format and shows the result", async () => {
    renderCard();
    const textarea = screen.getByTestId(`${testId}-emails`);
    await waitFor(() =>
      expect(textarea).toHaveValue("a@ucsb.edu\nb@ucsb.edu\nc@ucsb.edu"),
    );

    fireEvent.change(screen.getByTestId(`${testId}-format`), {
      target: { value: "COMMA_SEPARATED" },
    });

    await waitFor(() => expect(textarea).toHaveValue(commaSeparated));
    expect(emailsGetHistory().length).toBe(2);
    expect(emailsGetHistory()[1].params).toEqual({
      courseId: 7,
      type: "STAFF",
      format: "COMMA_SEPARATED",
    });
    expect(screen.getByTestId(`${testId}-format`)).toHaveValue(
      "COMMA_SEPARATED",
    );

    fireEvent.change(screen.getByTestId(`${testId}-format`), {
      target: { value: "ONE_PER_LINE" },
    });
    await waitFor(() =>
      expect(textarea).toHaveValue("a@ucsb.edu\nb@ucsb.edu\nc@ucsb.edu"),
    );
  });

  test("the Copy button copies the emails to the clipboard and says so", async () => {
    const user = userEvent.setup();
    renderCard();

    const copy = screen.getByTestId(`${testId}-copy`);
    expect(copy).toHaveTextContent("Copy");
    await waitFor(() => expect(copy).not.toBeDisabled());

    await user.click(copy);

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith("Emails copied to the clipboard."),
    );
    expect(mockToast).toHaveBeenCalledTimes(1);
    expect(await navigator.clipboard.readText()).toBe(onePerLine);
  });

  test("the Copy button copies the comma separated format when that is selected", async () => {
    const user = userEvent.setup();
    renderCard();

    fireEvent.change(screen.getByTestId(`${testId}-format`), {
      target: { value: "COMMA_SEPARATED" },
    });
    await waitFor(() =>
      expect(screen.getByTestId(`${testId}-emails`)).toHaveValue(
        commaSeparated,
      ),
    );

    await user.click(screen.getByTestId(`${testId}-copy`));

    await waitFor(() => expect(mockToast).toHaveBeenCalled());
    expect(await navigator.clipboard.readText()).toBe(commaSeparated);
  });

  test("says so when the emails cannot be copied", async () => {
    const user = userEvent.setup();
    renderCard();
    const copy = screen.getByTestId(`${testId}-copy`);
    await waitFor(() => expect(copy).not.toBeDisabled());
    vi.spyOn(navigator.clipboard, "writeText").mockRejectedValue(
      new Error("denied"),
    );

    await user.click(copy);

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith(
        "Unable to copy the emails to the clipboard.",
      ),
    );
    expect(mockToast).toHaveBeenCalledTimes(1);
  });

  test("the Copy button is disabled when there are no emails", async () => {
    axiosMock.reset();
    axiosMock.onGet(emailsUrl).reply(200, "");

    renderCard();

    await waitFor(() => expect(emailsGetHistory().length).toBe(1));
    expect(screen.getByTestId(`${testId}-emails`)).toHaveValue("");
    expect(screen.getByTestId(`${testId}-copy`)).toBeDisabled();
  });

  test("a failed request does not toast and leaves the text area empty", async () => {
    axiosMock.reset();
    axiosMock.onGet(emailsUrl).reply(500);

    renderCard();

    await waitFor(() => expect(emailsGetHistory().length).toBe(1));
    expect(mockToast).not.toHaveBeenCalled();
    expect(screen.getByTestId(`${testId}-emails`)).toHaveValue("");
    expect(screen.getByTestId(`${testId}-copy`)).toBeDisabled();
  });

  test("uses a custom testIdPrefix", () => {
    renderCard({ testIdPrefix: "Custom" });

    expect(screen.getByTestId("Custom-card")).toBeInTheDocument();
    expect(screen.getByTestId("Custom-format")).toBeInTheDocument();
    expect(screen.getByTestId("Custom-emails")).toBeInTheDocument();
    expect(screen.getByTestId("Custom-copy")).toBeInTheDocument();
  });
});
