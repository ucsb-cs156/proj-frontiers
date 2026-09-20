import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { toast } from "react-toastify";

import SectionsTabComponent from "main/components/TabComponent/SectionsTabComponent";
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
const testId = "InstructorCourseShowPage";

const renderTab = () =>
  render(
    <QueryClientProvider client={queryClient}>
      <SectionsTabComponent courseId={1} testIdPrefix={testId} />
    </QueryClientProvider>,
  );

describe("SectionsTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    vi.resetAllMocks();
  });

  test("renders sections from the backend along with create button", async () => {
    axiosMock
      .onGet("/api/courses/1/sections")
      .reply(200, sectionsFixtures.threeSections);

    renderTab();

    expect(
      screen.getByTestId(`${testId}-sections-tab-component`),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-create-section-button`),
    ).toHaveTextContent("Create Section");
    expect(screen.getByTestId(`${testId}-sections-table`)).toBeInTheDocument();

    expect(
      await screen.findByTestId(
        `${testId}-sections-table-cell-row-0-col-section`,
      ),
    ).toHaveTextContent("0100");
    expect(
      screen.getByTestId(`${testId}-sections-table-cell-row-2-col-label`),
    ).toHaveTextContent("Tue 11:00am");
    expect(
      screen.getByTestId(`${testId}-sections-table-cell-row-0-col-Edit-button`),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(
        `${testId}-sections-table-cell-row-0-col-Delete-button`,
      ),
    ).toBeInTheDocument();

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe("/api/courses/1/sections");
    expect(
      screen.queryByText("Create Section", { selector: ".modal-title" }),
    ).not.toBeInTheDocument();
  });

  test("create button opens modal, submitting sends POST and closes modal", async () => {
    axiosMock.onGet("/api/courses/1/sections").reply(200, []);
    axiosMock
      .onPost("/api/courses/1/sections")
      .reply(200, { id: 5, section: "0500", label: "Fri 3:00pm" });

    renderTab();

    fireEvent.click(screen.getByTestId(`${testId}-create-section-button`));

    expect(
      await screen.findByText("Create Section", { selector: ".modal-title" }),
    ).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-create-section-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );
    expect(screen.getByTestId("SectionsForm-section")).toHaveValue("");
    expect(screen.getByTestId("SectionsForm-submit")).toHaveTextContent(
      "Create",
    );

    fireEvent.change(screen.getByTestId("SectionsForm-section"), {
      target: { value: "0500" },
    });
    fireEvent.change(screen.getByTestId("SectionsForm-label"), {
      target: { value: "Fri 3:00pm" },
    });
    fireEvent.click(screen.getByTestId("SectionsForm-submit"));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].url).toBe("/api/courses/1/sections");
    expect(axiosMock.history.post[0].params).toEqual({
      section: "0500",
      label: "Fri 3:00pm",
    });

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith("Section successfully created."),
    );
    await waitFor(() =>
      expect(
        screen.queryByText("Create Section", { selector: ".modal-title" }),
      ).not.toBeInTheDocument(),
    );
    // the sections list is refetched after a successful create
    await waitFor(() => expect(axiosMock.history.get.length).toBe(2));
    expect(axiosMock.history.get[1].url).toBe("/api/courses/1/sections");
  });

  test("editing a section through the table refetches the sections list", async () => {
    axiosMock
      .onGet("/api/courses/1/sections")
      .reply(200, sectionsFixtures.threeSections);
    axiosMock
      .onPut("/api/courses/1/sections/1")
      .reply(200, { id: 1, section: "0100", label: "Tue 9:30am" });

    renderTab();

    fireEvent.click(
      await screen.findByTestId(
        `${testId}-sections-table-cell-row-0-col-Edit-button`,
      ),
    );
    expect(
      await screen.findByTestId(`${testId}-sections-table-edit-modal-body`),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByTestId("SectionsForm-label"), {
      target: { value: "Tue 9:30am" },
    });
    fireEvent.click(screen.getByTestId("SectionsForm-submit"));

    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    expect(axiosMock.history.put[0].params).toEqual({
      section: "0100",
      label: "Tue 9:30am",
    });
    await waitFor(() => expect(axiosMock.history.get.length).toBe(2));
    expect(axiosMock.history.get[1].url).toBe("/api/courses/1/sections");
  });

  test("deleting a section through the table refetches the sections list", async () => {
    axiosMock
      .onGet("/api/courses/1/sections")
      .reply(200, sectionsFixtures.threeSections);
    axiosMock
      .onDelete("/api/courses/1/sections/2")
      .reply(200, { message: "Section with id 2 deleted" });

    renderTab();

    fireEvent.click(
      await screen.findByTestId(
        `${testId}-sections-table-cell-row-1-col-Delete-button`,
      ),
    );
    expect(
      await screen.findByTestId("ConfirmationModal-base"),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByText("Yes, I'd like to do this"));

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].url).toBe("/api/courses/1/sections/2");
    await waitFor(() => expect(axiosMock.history.get.length).toBe(2));
    expect(axiosMock.history.get[1].url).toBe("/api/courses/1/sections");
  });

  test("create modal can be closed without submitting", async () => {
    axiosMock.onGet("/api/courses/1/sections").reply(200, []);

    renderTab();

    fireEvent.click(screen.getByTestId(`${testId}-create-section-button`));
    expect(
      await screen.findByText("Create Section", { selector: ".modal-title" }),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText("Close"));

    await waitFor(() =>
      expect(
        screen.queryByText("Create Section", { selector: ".modal-title" }),
      ).not.toBeInTheDocument(),
    );
    expect(axiosMock.history.post.length).toBe(0);
  });

  test("create shows duplicate message on 409 and keeps modal open", async () => {
    axiosMock
      .onGet("/api/courses/1/sections")
      .reply(200, sectionsFixtures.oneSection);
    axiosMock.onPost("/api/courses/1/sections").reply(409);

    renderTab();

    fireEvent.click(screen.getByTestId(`${testId}-create-section-button`));
    expect(
      await screen.findByText("Create Section", { selector: ".modal-title" }),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByTestId("SectionsForm-section"), {
      target: { value: "0100" },
    });
    fireEvent.change(screen.getByTestId("SectionsForm-label"), {
      target: { value: "Duplicate" },
    });
    fireEvent.click(screen.getByTestId("SectionsForm-submit"));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "A section with that section value already exists for this course.",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    expect(
      screen.getByText("Create Section", { selector: ".modal-title" }),
    ).toBeInTheDocument();
  });

  test("does not toast when the sections request fails", async () => {
    axiosMock.onGet("/api/courses/1/sections").reply(500);

    renderTab();

    await waitFor(() => expect(axiosMock.history.get.length).toBe(1));
    expect(toast).not.toHaveBeenCalled();
    expect(screen.getByTestId(`${testId}-sections-table`)).toBeInTheDocument();
  });
});
