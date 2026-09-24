import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { toast } from "react-toastify";

import DokkuAccountTranslationsTable from "main/components/Dokku/DokkuAccountTranslationsTable";
import { dokkuAccountTranslationsFixtures } from "fixtures/dokkuAccountTranslationsFixtures";

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const testId = "DokkuAccountTranslationsTable";
const formId = "DokkuAccountTranslationsForm";

const renderTable = (props = {}) =>
  render(
    <QueryClientProvider client={queryClient}>
      <DokkuAccountTranslationsTable
        translations={dokkuAccountTranslationsFixtures.threeTranslations}
        courseId={1}
        {...props}
      />
    </QueryClientProvider>,
  );

describe("DokkuAccountTranslationsTable tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    vi.resetAllMocks();
  });

  test("renders headers, rows and buttons with default testIdPrefix", () => {
    renderTable();

    expect(screen.getByTestId(testId)).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-header-email`)).toHaveTextContent(
      "Email",
    );
    expect(screen.getByTestId(`${testId}-header-username`)).toHaveTextContent(
      "Dokku Username",
    );
    expect(screen.getByTestId(`${testId}-header-Edit`)).toHaveTextContent(
      "Edit",
    );
    expect(screen.getByTestId(`${testId}-header-Delete`)).toHaveTextContent(
      "Delete",
    );
    expect(screen.queryByTestId(`${testId}-header-id`)).not.toBeInTheDocument();

    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-email`),
    ).toHaveTextContent("cgaucho@ucsb.edu");
    expect(
      screen.getByTestId(`${testId}-cell-row-0-col-username`),
    ).toHaveTextContent("chrisgaucho");
    expect(
      screen.getByTestId(`${testId}-cell-row-2-col-email`),
    ).toHaveTextContent("phtcon@ucsb.edu");
    expect(
      screen.getByTestId(`${testId}-cell-row-2-col-username`),
    ).toHaveTextContent("pconrad");

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

  test("renders an empty table", () => {
    renderTable({ translations: [] });
    expect(screen.getByTestId(`${testId}-header-email`)).toBeInTheDocument();
    expect(
      screen.queryByTestId(`${testId}-cell-row-0-col-email`),
    ).not.toBeInTheDocument();
  });

  test("uses custom testIdPrefix", () => {
    renderTable({ testIdPrefix: "Custom" });
    expect(screen.getByTestId("Custom")).toBeInTheDocument();
    expect(
      screen.getByTestId("Custom-cell-row-0-col-Edit-button"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("Custom-cell-row-0-col-Delete-button"),
    ).toBeInTheDocument();
  });

  test("edit button opens modal prefilled with the email locked, submitting sends PUT and closes modal", async () => {
    axiosMock
      .onPut("/api/dokku/translations")
      .reply(200, { email: "ldelplaya@ucsb.edu", username: "lauren2" });

    renderTable();

    fireEvent.click(screen.getByTestId(`${testId}-cell-row-1-col-Edit-button`));

    expect(
      await screen.findByTestId(`${testId}-edit-modal-body`),
    ).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-edit-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );
    expect(
      screen.getByText("Edit Dokku Account Translation"),
    ).toBeInTheDocument();
    expect(screen.getByTestId(`${formId}-email`)).toHaveValue(
      "ldelplaya@ucsb.edu",
    );
    expect(screen.getByTestId(`${formId}-email`)).toBeDisabled();
    expect(screen.getByTestId(`${formId}-username`)).toHaveValue("lauren");
    expect(screen.getByTestId(`${formId}-submit`)).toHaveTextContent("Update");

    fireEvent.change(screen.getByTestId(`${formId}-username`), {
      target: { value: "lauren2" },
    });
    fireEvent.click(screen.getByTestId(`${formId}-submit`));

    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    expect(axiosMock.history.put[0].url).toBe("/api/dokku/translations");
    expect(axiosMock.history.put[0].params).toEqual({
      courseId: 1,
      email: "ldelplaya@ucsb.edu",
      username: "lauren2",
    });

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "Dokku account translation updated successfully.",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
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
    expect(toast).not.toHaveBeenCalled();
  });

  test("edit shows duplicate message on 409 and keeps modal open", async () => {
    axiosMock.onPut("/api/dokku/translations").reply(409);

    renderTable();

    fireEvent.click(screen.getByTestId(`${testId}-cell-row-0-col-Edit-button`));
    expect(
      await screen.findByTestId(`${testId}-edit-modal-body`),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByTestId(`${formId}-username`), {
      target: { value: "other" },
    });
    fireEvent.click(screen.getByTestId(`${formId}-submit`));

    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "A dokku account translation for that email already exists.",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId(`${testId}-edit-modal-body`)).toBeInTheDocument();
  });

  test("edit shows the error on other failures and keeps modal open", async () => {
    axiosMock.onPut("/api/dokku/translations").reply(500);

    renderTable();

    fireEvent.click(screen.getByTestId(`${testId}-cell-row-0-col-Edit-button`));
    expect(
      await screen.findByTestId(`${testId}-edit-modal-body`),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByTestId(`${formId}-username`), {
      target: { value: "other" },
    });
    fireEvent.click(screen.getByTestId(`${formId}-submit`));

    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "Error: Request failed with status code 500",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId(`${testId}-edit-modal-body`)).toBeInTheDocument();
  });

  test("delete button opens confirmation, confirming sends DELETE", async () => {
    axiosMock.onDelete("/api/dokku/translations").reply(200, {
      message: "Dokku account translation for phtcon@ucsb.edu deleted",
    });

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
      "Are you sure you want to delete the dokku account translation for phtcon@ucsb.edu (pconrad)?",
    );

    fireEvent.click(screen.getByText("Yes, I'd like to do this"));

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].url).toBe("/api/dokku/translations");
    expect(axiosMock.history.delete[0].params).toEqual({
      courseId: 1,
      email: "phtcon@ucsb.edu",
    });
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "Dokku account translation deleted successfully.",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
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
