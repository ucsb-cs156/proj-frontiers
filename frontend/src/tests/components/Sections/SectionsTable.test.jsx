import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { toast } from "react-toastify";

import SectionsTable from "main/components/Sections/SectionsTable";
import { sectionsFixtures } from "fixtures/sectionsFixtures";

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const testId = "SectionsTable";

const renderTable = (props = {}) =>
  render(
    <QueryClientProvider client={queryClient}>
      <SectionsTable
        sections={sectionsFixtures.threeSections}
        courseId={1}
        {...props}
      />
    </QueryClientProvider>,
  );

describe("SectionsTable tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    vi.resetAllMocks();
  });

  test("renders headers, rows and buttons with default testIdPrefix", () => {
    renderTable();

    expect(screen.getByTestId(testId)).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-header-id`)).toHaveTextContent("id");
    expect(screen.getByTestId(`${testId}-header-section`)).toHaveTextContent(
      "Section",
    );
    expect(screen.getByTestId(`${testId}-header-label`)).toHaveTextContent(
      "Label",
    );
    expect(screen.getByTestId(`${testId}-header-Edit`)).toHaveTextContent(
      "Edit",
    );
    expect(screen.getByTestId(`${testId}-header-Delete`)).toHaveTextContent(
      "Delete",
    );

    expect(screen.getByTestId(`${testId}-cell-row-0-col-id`)).toHaveTextContent(
      "1",
    );
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-section`),
    ).toHaveTextContent("0100");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-label`),
    ).toHaveTextContent("Tue 9:00am");
    expect(
      screen.getByTestId(`${testId}-cell-row-2-col-section`),
    ).toHaveTextContent("0300");

    const editButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Edit-button`,
    );
    expect(editButton).toHaveTextContent("Edit");
    expect(editButton).toHaveClass("btn-primary");
    const deleteButton = screen.getByTestId(
      `${testId}-cell-row-0-col-Delete-button`,
    );
    expect(deleteButton).toHaveTextContent("Delete");
    expect(deleteButton).toHaveClass("btn-danger");

    expect(
      screen.queryByTestId(`${testId}-edit-modal-body`),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("ConfirmationModal-base"),
    ).not.toBeInTheDocument();
  });

  test("uses custom testIdPrefix", () => {
    renderTable({ testIdPrefix: "Custom" });
    expect(screen.getByTestId("Custom")).toBeInTheDocument();
    expect(
      screen.getByTestId("Custom-cell-row-0-col-Edit-button"),
    ).toBeInTheDocument();
  });

  test("edit button opens modal prefilled, submitting sends PUT and closes modal", async () => {
    axiosMock
      .onPut("/api/courses/1/sections/2")
      .reply(200, { id: 2, section: "0250", label: "Tue 10:30am" });

    renderTable();

    fireEvent.click(screen.getByTestId(`${testId}-cell-row-1-col-Edit-button`));

    expect(
      await screen.findByTestId(`${testId}-edit-modal-body`),
    ).toBeInTheDocument();
    expect(screen.getByText("Edit Section")).toBeInTheDocument();
    expect(screen.getByTestId("SectionsForm-section")).toHaveValue("0200");
    expect(screen.getByTestId("SectionsForm-label")).toHaveValue("Tue 10:00am");
    expect(screen.getByTestId("SectionsForm-submit")).toHaveTextContent(
      "Update",
    );

    fireEvent.change(screen.getByTestId("SectionsForm-section"), {
      target: { value: "0250" },
    });
    fireEvent.change(screen.getByTestId("SectionsForm-label"), {
      target: { value: "Tue 10:30am" },
    });
    fireEvent.click(screen.getByTestId("SectionsForm-submit"));

    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    expect(axiosMock.history.put[0].url).toBe("/api/courses/1/sections/2");
    expect(axiosMock.history.put[0].params).toEqual({
      section: "0250",
      label: "Tue 10:30am",
    });

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith("Section updated successfully."),
    );
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-edit-modal-body`),
      ).not.toBeInTheDocument(),
    );
  });

  test("edit modal can be closed without submitting", async () => {
    renderTable();

    fireEvent.click(screen.getByTestId(`${testId}-cell-row-0-col-Edit-button`));
    expect(
      await screen.findByTestId(`${testId}-edit-modal-body`),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText("Close"));

    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-edit-modal-body`),
      ).not.toBeInTheDocument(),
    );
    expect(axiosMock.history.put.length).toBe(0);
  });

  test("edit shows duplicate message on 409 and keeps modal open", async () => {
    axiosMock.onPut("/api/courses/1/sections/1").reply(409);

    renderTable();

    fireEvent.click(screen.getByTestId(`${testId}-cell-row-0-col-Edit-button`));
    expect(
      await screen.findByTestId(`${testId}-edit-modal-body`),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByTestId("SectionsForm-section"), {
      target: { value: "0200" },
    });
    fireEvent.click(screen.getByTestId("SectionsForm-submit"));

    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "A section with that section value already exists for this course.",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId(`${testId}-edit-modal-body`)).toBeInTheDocument();
  });

  test("delete button opens confirmation, confirming sends DELETE", async () => {
    axiosMock
      .onDelete("/api/courses/1/sections/3")
      .reply(200, { message: "Section with id 3 deleted" });

    renderTable();

    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-2-col-Delete-button`),
    );

    expect(
      await screen.findByTestId("ConfirmationModal-base"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-delete-confirmation-message`),
    ).toHaveTextContent(
      "Are you sure you want to delete section 0300 (Tue 11:00am)?",
    );

    fireEvent.click(screen.getByText("Yes, I'd like to do this"));

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].url).toBe("/api/courses/1/sections/3");
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith("Section deleted successfully."),
    );
    await waitFor(() =>
      expect(
        screen.queryByTestId("ConfirmationModal-base"),
      ).not.toBeInTheDocument(),
    );
  });

  test("delete confirmation can be declined", async () => {
    renderTable();

    fireEvent.click(
      screen.getByTestId(`${testId}-cell-row-0-col-Delete-button`),
    );
    expect(
      await screen.findByTestId("ConfirmationModal-base"),
    ).toBeInTheDocument();

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
