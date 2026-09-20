import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import SectionsForm from "main/components/Sections/SectionsForm";
import { sectionsFixtures } from "fixtures/sectionsFixtures";

describe("SectionsForm tests", () => {
  test("renders correctly with no initialContents", () => {
    render(<SectionsForm submitAction={vi.fn()} />);

    expect(screen.getByText("Section")).toBeInTheDocument();
    expect(screen.getByText("Label")).toBeInTheDocument();
    expect(screen.getByTestId("SectionsForm-section")).toHaveValue("");
    expect(screen.getByTestId("SectionsForm-label")).toHaveValue("");
    expect(screen.getByTestId("SectionsForm-submit")).toHaveTextContent(
      "Create",
    );
  });

  test("renders correctly with initialContents and custom button label", () => {
    render(
      <SectionsForm
        submitAction={vi.fn()}
        initialContents={sectionsFixtures.oneSection[0]}
        buttonLabel="Update"
      />,
    );

    expect(screen.getByTestId("SectionsForm-section")).toHaveValue("0100");
    expect(screen.getByTestId("SectionsForm-label")).toHaveValue("Tue 9:00am");
    expect(screen.getByTestId("SectionsForm-submit")).toHaveTextContent(
      "Update",
    );
  });

  test("shows validation errors and does not submit when fields are empty", async () => {
    const submitAction = vi.fn();
    render(<SectionsForm submitAction={submitAction} />);

    fireEvent.click(screen.getByTestId("SectionsForm-submit"));

    await screen.findByText("Section is required.");
    expect(screen.getByText("Label is required.")).toBeInTheDocument();
    expect(submitAction).not.toHaveBeenCalled();
  });

  test("calls submitAction with the entered values", async () => {
    const submitAction = vi.fn();
    render(<SectionsForm submitAction={submitAction} />);

    fireEvent.change(screen.getByTestId("SectionsForm-section"), {
      target: { value: "0400" },
    });
    fireEvent.change(screen.getByTestId("SectionsForm-label"), {
      target: { value: "Wed 2:00pm" },
    });
    fireEvent.click(screen.getByTestId("SectionsForm-submit"));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction.mock.calls[0][0]).toEqual({
      section: "0400",
      label: "Wed 2:00pm",
    });
    expect(screen.queryByText("Section is required.")).not.toBeInTheDocument();
    expect(screen.queryByText("Label is required.")).not.toBeInTheDocument();
  });
});
