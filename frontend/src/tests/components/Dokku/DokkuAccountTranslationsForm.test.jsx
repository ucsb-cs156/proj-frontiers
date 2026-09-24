import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import DokkuAccountTranslationsForm from "main/components/Dokku/DokkuAccountTranslationsForm";
import { dokkuAccountTranslationsFixtures } from "fixtures/dokkuAccountTranslationsFixtures";

const testId = "DokkuAccountTranslationsForm";

describe("DokkuAccountTranslationsForm tests", () => {
  test("renders correctly with no initialContents", () => {
    render(<DokkuAccountTranslationsForm submitAction={vi.fn()} />);

    expect(screen.getByText("Email")).toBeInTheDocument();
    expect(screen.getByText("Dokku Username")).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-email`)).toHaveValue("");
    expect(screen.getByTestId(`${testId}-email`)).not.toBeDisabled();
    expect(screen.getByTestId(`${testId}-username`)).toHaveValue("");
    expect(screen.getByTestId(`${testId}-submit`)).toHaveTextContent("Create");
  });

  test("renders correctly with initialContents and custom button label, with the email locked", () => {
    render(
      <DokkuAccountTranslationsForm
        submitAction={vi.fn()}
        initialContents={dokkuAccountTranslationsFixtures.oneTranslation[0]}
        buttonLabel="Update"
      />,
    );

    expect(screen.getByTestId(`${testId}-email`)).toHaveValue(
      "cgaucho@ucsb.edu",
    );
    expect(screen.getByTestId(`${testId}-email`)).toBeDisabled();
    expect(screen.getByTestId(`${testId}-username`)).toHaveValue("chrisgaucho");
    expect(screen.getByTestId(`${testId}-username`)).not.toBeDisabled();
    expect(screen.getByTestId(`${testId}-submit`)).toHaveTextContent("Update");
  });

  test("shows validation errors and does not submit when fields are empty", async () => {
    const submitAction = vi.fn();
    render(<DokkuAccountTranslationsForm submitAction={submitAction} />);

    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await screen.findByText("Email is required.");
    expect(screen.getByText("Dokku Username is required.")).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-email`)).toHaveClass("is-invalid");
    expect(screen.getByTestId(`${testId}-username`)).toHaveClass("is-invalid");
    expect(submitAction).not.toHaveBeenCalled();
  });

  test("calls submitAction with the entered values", async () => {
    const submitAction = vi.fn();
    render(<DokkuAccountTranslationsForm submitAction={submitAction} />);

    fireEvent.change(screen.getByTestId(`${testId}-email`), {
      target: { value: "newperson@ucsb.edu" },
    });
    fireEvent.change(screen.getByTestId(`${testId}-username`), {
      target: { value: "newperson2" },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction.mock.calls[0][0]).toEqual({
      email: "newperson@ucsb.edu",
      username: "newperson2",
    });
    expect(screen.queryByText("Email is required.")).not.toBeInTheDocument();
    expect(
      screen.queryByText("Dokku Username is required."),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-email`)).not.toHaveClass("is-invalid");
    expect(screen.getByTestId(`${testId}-username`)).not.toHaveClass(
      "is-invalid",
    );
  });

  test("when editing, submits the locked email along with the new username", async () => {
    const submitAction = vi.fn();
    render(
      <DokkuAccountTranslationsForm
        submitAction={submitAction}
        initialContents={dokkuAccountTranslationsFixtures.oneTranslation[0]}
        buttonLabel="Update"
      />,
    );

    fireEvent.change(screen.getByTestId(`${testId}-username`), {
      target: { value: "cgaucho2" },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction.mock.calls[0][0]).toEqual({
      email: "cgaucho@ucsb.edu",
      username: "cgaucho2",
    });
  });
});
