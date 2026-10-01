import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";

import ApiKeyCreateForm from "main/components/ApiKeys/ApiKeyCreateForm";

const testId = "ApiKeyCreateForm";

describe("ApiKeyCreateForm tests", () => {
  test("renders the label field, the expiry choices and the submit button", () => {
    render(<ApiKeyCreateForm submitAction={vi.fn()} />);

    const label = screen.getByLabelText("Label (optional)");
    expect(label).toBe(screen.getByTestId(`${testId}-label`));
    expect(label).toHaveAttribute("placeholder", "e.g. jpa02 autograder F26");
    expect(label).toHaveValue("");
    expect(
      screen.getByText(
        "A short name, so that you can tell this key apart from others later.",
      ),
    ).toBeInTheDocument();

    expect(screen.getByText("Expires after")).toBeInTheDocument();
    const ninetyDays = screen.getByLabelText("90 days");
    const sixMonths = screen.getByLabelText("6 months");
    expect(ninetyDays).toBe(screen.getByTestId(`${testId}-choice-DAYS_90`));
    expect(sixMonths).toBe(screen.getByTestId(`${testId}-choice-MONTHS_6`));
    expect(ninetyDays).toHaveAttribute("type", "radio");
    expect(ninetyDays).toHaveAttribute("value", "DAYS_90");
    expect(sixMonths).toHaveAttribute("value", "MONTHS_6");
    expect(ninetyDays).toBeChecked();
    expect(sixMonths).not.toBeChecked();

    expect(screen.getByTestId(`${testId}-submit`)).toHaveTextContent(
      "Create API Key",
    );
    expect(screen.getByTestId(`${testId}-submit`)).toHaveAttribute(
      "type",
      "submit",
    );
  });

  test("submits the trimmed label and the default 90 day choice", async () => {
    const submitAction = vi.fn();
    render(<ApiKeyCreateForm submitAction={submitAction} />);

    fireEvent.change(screen.getByTestId(`${testId}-label`), {
      target: { value: "  jpa02 autograder F26  " },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction).toHaveBeenCalledWith({
      label: "jpa02 autograder F26",
      choice: "DAYS_90",
    });
  });

  test("submits an empty label and the 6 month choice", async () => {
    const submitAction = vi.fn();
    render(<ApiKeyCreateForm submitAction={submitAction} />);

    fireEvent.click(screen.getByTestId(`${testId}-choice-MONTHS_6`));
    expect(screen.getByTestId(`${testId}-choice-MONTHS_6`)).toBeChecked();
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction).toHaveBeenCalledWith({
      label: "",
      choice: "MONTHS_6",
    });
  });

  test("rejects a label longer than 60 characters, and accepts exactly 60", async () => {
    const submitAction = vi.fn();
    render(<ApiKeyCreateForm submitAction={submitAction} />);

    fireEvent.change(screen.getByTestId(`${testId}-label`), {
      target: { value: "x".repeat(61) },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    expect(
      await screen.findByText("Label must be at most 60 characters."),
    ).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-label`)).toHaveClass("is-invalid");
    expect(submitAction).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId(`${testId}-label`), {
      target: { value: "y".repeat(60) },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction).toHaveBeenCalledWith({
      label: "y".repeat(60),
      choice: "DAYS_90",
    });
    await waitFor(() =>
      expect(
        screen.queryByText("Label must be at most 60 characters."),
      ).not.toBeInTheDocument(),
    );
    expect(screen.getByTestId(`${testId}-label`)).not.toHaveClass("is-invalid");
  });

  test("uses a custom testIdPrefix", () => {
    render(<ApiKeyCreateForm submitAction={vi.fn()} testIdPrefix="Custom" />);
    expect(screen.getByTestId("Custom-label")).toBeInTheDocument();
    expect(screen.getByTestId("Custom-choice-DAYS_90")).toBeInTheDocument();
    expect(screen.getByTestId("Custom-choice-MONTHS_6")).toBeInTheDocument();
    expect(screen.getByTestId("Custom-submit")).toBeInTheDocument();
  });
});
