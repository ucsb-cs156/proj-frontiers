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
    const headers = ["id", "Student Id", "First Name", "Last Name", "Email"];
    const accessors = ["id", "studentId", "firstName", "lastName", "email"];
    headers.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });
    accessors.forEach((accessor) => {
      expect(
        screen.getByTestId(`DroppedStudentsTable-cell-row-0-col-${accessor}`),
      ).toBeInTheDocument();
    });
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
});
