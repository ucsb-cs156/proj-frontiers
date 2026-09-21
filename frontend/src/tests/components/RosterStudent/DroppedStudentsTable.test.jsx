import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { expect, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import DroppedStudentsTable from "main/components/RosterStudent/DroppedStudentsTable";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});
describe("DroppedStudentsTable tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    vi.clearAllMocks();
  });
  test("renders correctly", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <DroppedStudentsTable
          students={rosterStudentFixtures.threeStudents}
          courseId={1}
        />
        ,
      </QueryClientProvider>,
    );
    const headers = [
      "id",
      "Student Id",
      "First Name",
      "Last Name",
      "Email",
      "Section",
    ];
    const accessors = [
      "id",
      "studentId",
      "firstName",
      "lastName",
      "email",
      "section",
    ];
    expect(
      screen.getByTestId("DroppedStudentsTable-header-Restore"),
    ).toHaveTextContent("Restore");
    expect(
      screen.getByTestId("DroppedStudentsTable-header-Delete"),
    ).toHaveTextContent("Delete");
    headers.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });
    accessors.forEach((accessor) => {
      expect(
        screen.getByTestId(`DroppedStudentsTable-cell-row-0-col-${accessor}`),
      ).toBeInTheDocument();
    });
    expect(
      screen.getByTestId("DroppedStudentsTable-cell-row-0-col-id"),
    ).toHaveTextContent("3");
    const deleteButton = screen.getByTestId(
      "DeleteDroppedButton-cell-row-0-col-Delete-button",
    );
    expect(deleteButton).toHaveTextContent("Delete");
    expect(deleteButton).toHaveClass("btn-danger");
    expect(
      screen.queryByTestId("RosterStudentDeleteModal"),
    ).not.toBeInTheDocument();
  });
  test("restore works correctly", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    queryClientSpecific.setQueryData(
      ["/api/rosterstudents/course/7"],
      rosterStudentFixtures.threeStudents,
    );
    queryClientSpecific.setQueryData(["mock queryData"], null);
    axiosMock.onPut("/api/rosterstudents/restore").reply(200);
    render(
      <QueryClientProvider client={queryClientSpecific}>
        <DroppedStudentsTable
          students={rosterStudentFixtures.threeStudents}
          courseId={7}
        />
        ,
      </QueryClientProvider>,
    );
    const restoreButton = await screen.findByTestId(
      "RestoreButton-cell-row-0-col-Restore-button",
    );
    expect(restoreButton).toHaveClass("btn-primary");
    fireEvent.click(restoreButton);
    await waitFor(() => expect(axiosMock.history.put.length).toEqual(1));
    expect(mockToast).toBeCalledWith(
      "Student successfully restored to course.",
    );
    expect(axiosMock.history.put.length).toEqual(1);
    expect(axiosMock.history.put[0].params).toEqual({
      id: 3,
    });
    expect(
      queryClientSpecific.getQueryState(["/api/rosterstudents/course/7"])
        .isInvalidated,
    ).toBe(true);
    expect(
      queryClientSpecific.getQueryState(["mock queryData"]).isInvalidated,
    ).toBe(false);
  });

  test("delete opens confirmation modal and sends DELETE with chosen org option", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    queryClientSpecific.setQueryData(
      ["/api/rosterstudents/course/7"],
      rosterStudentFixtures.threeStudents,
    );
    queryClientSpecific.setQueryData(["mock queryData"], null);
    axiosMock.onDelete("/api/rosterstudents/delete").reply(200);
    render(
      <QueryClientProvider client={queryClientSpecific}>
        <DroppedStudentsTable
          students={rosterStudentFixtures.threeStudents}
          courseId={7}
        />
      </QueryClientProvider>,
    );

    fireEvent.click(
      screen.getByTestId("DeleteDroppedButton-cell-row-1-col-Delete-button"),
    );
    await screen.findByTestId("RosterStudentDeleteModal");
    fireEvent.click(
      screen.getByLabelText(
        "Yes, I'd like to remove them from the GitHub Organization",
      ),
    );
    fireEvent.click(screen.getByText("Delete Student"));

    await waitFor(() => expect(axiosMock.history.delete.length).toEqual(1));
    expect(axiosMock.history.delete[0].url).toBe("/api/rosterstudents/delete");
    expect(axiosMock.history.delete[0].params).toEqual({
      id: 4,
      removeFromOrg: "true",
    });
    await waitFor(() =>
      expect(mockToast).toBeCalledWith("Student deleted successfully."),
    );
    await waitFor(() =>
      expect(
        screen.queryByTestId("RosterStudentDeleteModal"),
      ).not.toBeInTheDocument(),
    );
    expect(
      queryClientSpecific.getQueryState(["/api/rosterstudents/course/7"])
        .isInvalidated,
    ).toBe(true);
    expect(
      queryClientSpecific.getQueryState(["mock queryData"]).isInvalidated,
    ).toBe(false);
    expect(axiosMock.history.put.length).toEqual(0);
  });

  test("delete modal can be closed without deleting", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <DroppedStudentsTable
          students={rosterStudentFixtures.threeStudents}
          courseId={7}
        />
      </QueryClientProvider>,
    );

    fireEvent.click(
      screen.getByTestId("DeleteDroppedButton-cell-row-0-col-Delete-button"),
    );
    await screen.findByTestId("RosterStudentDeleteModal");
    fireEvent.click(screen.getByRole("button", { name: "Close" }));
    await waitFor(() =>
      expect(
        screen.queryByTestId("RosterStudentDeleteModal"),
      ).not.toBeInTheDocument(),
    );
    expect(axiosMock.history.delete.length).toEqual(0);
    expect(mockToast).not.toBeCalled();
  });
});
