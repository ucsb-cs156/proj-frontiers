import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import StaffCourseShowPage from "main/pages/Staff/StaffCourseShowPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import coursesFixtures from "fixtures/coursesFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";
import { teamsFixtures } from "fixtures/TeamsFixtures";
import { newAssignmentsFixtures } from "fixtures/newAssignmentsFixtures";
import { expect, vi } from "vitest";

const mockedNavigate = vi.fn();
vi.mock("react-router", async (importOriginal) => ({
  ...(await importOriginal()),
  useNavigate: () => mockedNavigate,
}));

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

describe("StaffCourseShowPage tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    window.localStorage.clear();
    mockedNavigate.mockClear();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
    axiosMock.onGet("/api/jobs/course").reply(200, []);
    axiosMock.onGet("/api/courses/warnings/7").reply(200, {
      showOrganizationAgeWarning: false,
    });
    axiosMock
      .onGet("/api/rosterstudents/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);
    axiosMock
      .onGet("/api/coursestaff/course?courseId=7")
      .reply(200, courseStaffFixtures.staffWithEachStatus);
    axiosMock
      .onGet("/api/teams/all?courseId=7")
      .reply(200, teamsFixtures.teams);
  });

  const renderPage = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/staff/courses/7"]}>
          <Routes>
            <Route
              path="/staff/courses/:id"
              element={<StaffCourseShowPage />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );
  };

  test("renders course header and staff-appropriate tabs", async () => {
    axiosMock.onGet("/api/courses/7").reply(200, {
      ...coursesFixtures.oneCourseWithEachStatus[0],
      id: 7,
      installationId: "123456789",
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId("StaffCourseShowPage-title")).toHaveTextContent(
        "CMPSC 156",
      );
    });

    expect(screen.getByRole("tab", { name: "Students" })).toHaveAttribute(
      "data-rr-ui-event-key",
      "students",
    );
    expect(screen.getByRole("tab", { name: "Staff" })).toHaveAttribute(
      "data-rr-ui-event-key",
      "staff",
    );
    expect(screen.getByRole("tab", { name: "Teams" })).toHaveAttribute(
      "data-rr-ui-event-key",
      "teams",
    );
    expect(screen.getByRole("tab", { name: "Assignments" })).toHaveAttribute(
      "data-rr-ui-event-key",
      "assignments",
    );
    expect(screen.getByRole("tab", { name: "Jobs" })).toHaveAttribute(
      "data-rr-ui-event-key",
      "jobs",
    );
    expect(
      screen.queryByRole("tab", { name: "Settings" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("StaffCourseShowPage-canvasForm"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("Delete Course")).not.toBeInTheDocument();
  });

  test("returns to course list when course lookup fails", async () => {
    vi.useFakeTimers({
      shouldAdvanceTime: true,
      toFake: ["setTimeout", "clearTimeout"],
    });
    axiosMock.onGet("/api/courses/7").timeout();

    const { unmount } = renderPage();

    expect(await screen.findByText("Course Not Found")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Close" }));
    vi.advanceTimersByTime(3000);
    expect(mockedNavigate).toHaveBeenCalledWith("/", { replace: true });

    unmount();
    vi.useRealTimers();
  });

  test("staff tab is visible but does not allow staff add edit or delete", async () => {
    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.oneCourseWithEachStatus[0]);
    renderPage();

    fireEvent.click(await screen.findByRole("tab", { name: "Staff" }));

    await waitFor(() => {
      expect(
        screen.getByTestId(
          "StaffCourseShowPage-CourseStaffTable-cell-row-0-col-id",
        ),
      ).toBeInTheDocument();
    });

    expect(screen.queryByText("Add Staff Member")).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(
        "StaffCourseShowPage-CourseStaffTable-cell-row-0-col-Edit-button",
      ),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(
        "StaffCourseShowPage-CourseStaffTable-cell-row-0-col-Delete-button",
      ),
    ).not.toBeInTheDocument();
  });

  test("staff users can still manage students and teams", async () => {
    axiosMock
      .onGet("/api/courses/7")
      .reply(200, coursesFixtures.oneCourseWithEachStatus[0]);
    axiosMock
      .onGet("/api/rosterstudents/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);
    axiosMock
      .onGet("/api/teams/all?courseId=7")
      .reply(200, teamsFixtures.teams);

    renderPage();

    fireEvent.click(await screen.findByRole("tab", { name: "Students" }));
    await waitFor(() => {
      expect(
        screen.getByTestId(
          "StaffCourseShowPage-RosterStudentTable-cell-row-0-col-Edit-button",
        ),
      ).toBeInTheDocument();
    });
    expect(
      screen.getByTestId(
        "StaffCourseShowPage-RosterStudentTable-cell-row-0-col-Delete-button",
      ),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: "Teams" }));
    await waitFor(() => {
      expect(
        screen.getByTestId(
          "StaffCourseShowPage-teams-table-3-add-member-button",
        ),
      ).toBeInTheDocument();
    });
    expect(
      screen.getByTestId("StaffCourseShowPage-teams-table-3-delete-button"),
    ).toBeInTheDocument();
  });
  describe("course options and the New Assignments tab", () => {
    const allOptions = {
      ENABLE_CANVAS: true,
      TRANSLATE_SECTIONS: true,
      DOKKU_MANAGER: true,
      ENABLE_API_KEYS: true,
      SLACK_INTEGRATION: true,
      NEW_ASSIGNMENT_FEATURES: true,
    };

    const setupCourse = (options = allOptions) => {
      axiosMock.onGet("/api/courses/7").reply(200, {
        ...coursesFixtures.oneCourseWithEachStatus[0],
        id: 7,
      });
      axiosMock.onGet("/api/course/options").reply(200, options);
      axiosMock
        .onGet("/api/assignments")
        .reply(200, newAssignmentsFixtures.threeAssignments);
      axiosMock.onGet("/api/jobs/course/logs/tail").reply(200, {
        status: "complete",
        lines: [
          { id: 1, jobId: 12, message: "Creating lab01-cgaucho" },
          { id: 2, jobId: 12, message: "Done" },
        ],
      });
    };

    const openNewAssignmentsTab = async () => {
      const tab = await screen.findByRole("tab", { name: "New Assignments" });
      fireEvent.click(tab);
      await screen.findByTestId(
        "StaffCourseShowPage-new-assignments-tab-component",
      );
    };

    const tableId = "StaffCourseShowPage-new-assignments-table";

    test("staff read the course options, and get the New Assignments tab when the option is on", async () => {
      setupCourse();

      renderPage();

      const tab = await screen.findByRole("tab", { name: "New Assignments" });
      expect(tab).toHaveAttribute("data-rr-ui-event-key", "new-assignments");
      const optionsRequests = axiosMock.history.get.filter(
        (request) => request.url === "/api/course/options",
      );
      expect(optionsRequests.length).toBe(1);
      expect(optionsRequests[0].params).toEqual({ courseId: "7" });
    });

    test("staff get no other tab that depends on an option, and no Settings tab, whatever the options are", async () => {
      setupCourse();

      renderPage();

      await screen.findByRole("tab", { name: "New Assignments" });
      const tabNames = screen.getAllByRole("tab").map((tab) => tab.textContent);
      expect(tabNames).toEqual([
        "Students",
        "Staff",
        "Teams",
        "Assignments",
        "New Assignments",
        "Jobs",
        "Downloads",
      ]);
      // nor the other things that options turn on
      expect(
        screen.queryByText(/Load Students from Canvas/),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByRole("tab", { name: "API Keys" }),
      ).not.toBeInTheDocument();
      // and nothing that those tabs load is asked for
      expect(
        axiosMock.history.get.some(
          (request) =>
            request.url.startsWith("/api/courses/slack") ||
            request.url === "/api/dokku/translations" ||
            request.url === "/api/courses/key" ||
            request.url === "/api/courses/7/sections",
        ),
      ).toBe(false);
    });

    test("there is no New Assignments tab when the option is off", async () => {
      setupCourse({ ...allOptions, NEW_ASSIGNMENT_FEATURES: false });

      renderPage();

      await waitFor(() => {
        expect(
          screen.getByTestId("StaffCourseShowPage-title"),
        ).toHaveTextContent("CMPSC 156");
      });
      await waitFor(() =>
        expect(
          axiosMock.history.get.some(
            (request) => request.url === "/api/course/options",
          ),
        ).toBe(true),
      );

      expect(
        screen.queryByRole("tab", { name: "New Assignments" }),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByRole("tab", { name: "Sections" }),
      ).not.toBeInTheDocument();
    });

    test("there is no New Assignments tab, and the page still works, when the options cannot be read", async () => {
      setupCourse();
      axiosMock.onGet("/api/course/options").reply(403);

      renderPage();

      await waitFor(() => {
        expect(
          screen.getByTestId("StaffCourseShowPage-title"),
        ).toHaveTextContent("CMPSC 156");
      });
      await waitFor(() =>
        expect(
          axiosMock.history.get.some(
            (request) => request.url === "/api/course/options",
          ),
        ).toBe(true),
      );
      expect(
        screen.queryByRole("tab", { name: "New Assignments" }),
      ).not.toBeInTheDocument();
      expect(screen.getByRole("tab", { name: "Students" })).toBeInTheDocument();
    });

    test("staff can see the list of assignments", async () => {
      setupCourse();
      renderPage();

      await openNewAssignmentsTab();

      expect(
        await screen.findByTestId(`${tableId}-cell-row-0-col-repoPrefix`),
      ).toHaveTextContent("lab01");
      expect(
        screen.getByTestId(`${tableId}-cell-row-2-col-repoPrefix`),
      ).toHaveTextContent("proj-team");
      expect(
        screen.getByTestId(`${tableId}-cell-row-2-col-teamRegex`),
      ).toHaveTextContent("s26-.*");
      const assignmentsRequests = axiosMock.history.get.filter(
        (request) => request.url === "/api/assignments",
      );
      expect(assignmentsRequests[0].params).toEqual({ courseId: "7" });
    });

    test("staff can add an individual assignment and a team assignment", async () => {
      setupCourse();
      axiosMock
        .onPost("/api/assignments/post")
        .reply(200, newAssignmentsFixtures.savedWithJob);
      renderPage();
      await openNewAssignmentsTab();

      fireEvent.click(
        await screen.findByTestId(
          "StaffCourseShowPage-create-individual-assignment-button",
        ),
      );
      fireEvent.change(
        await screen.findByTestId("NewIndividualAssignmentForm-repoPrefix"),
        { target: { value: "lab09" } },
      );
      fireEvent.click(screen.getByTestId("NewIndividualAssignmentForm-submit"));
      await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
      expect(axiosMock.history.post[0].params).toEqual({
        courseId: "7",
        asnType: "INDIVIDUAL",
        repoPrefix: "lab09",
        visibility: "PUBLIC",
        permission: "MAINTAIN",
        requireSignedCommit: false,
        createReposFor: "STUDENTS_ONLY",
      });

      await waitFor(() =>
        expect(
          screen.queryByTestId("NewIndividualAssignmentForm"),
        ).not.toBeInTheDocument(),
      );
      fireEvent.click(
        screen.getByTestId("StaffCourseShowPage-create-team-assignment-button"),
      );
      fireEvent.change(
        await screen.findByTestId("NewTeamAssignmentForm-repoPrefix"),
        { target: { value: "proj9" } },
      );
      fireEvent.click(screen.getByTestId("NewTeamAssignmentForm-submit"));
      await waitFor(() => expect(axiosMock.history.post.length).toBe(2));
      expect(axiosMock.history.post[1].params.asnType).toBe("TEAM");
      expect(axiosMock.history.post[1].params.repoPrefix).toBe("proj9");
    });

    test("staff can edit an assignment", async () => {
      setupCourse();
      axiosMock
        .onPut("/api/assignments/put")
        .reply(200, newAssignmentsFixtures.savedWithJob);
      renderPage();
      await openNewAssignmentsTab();

      fireEvent.click(
        await screen.findByTestId(`${tableId}-cell-row-0-col-Edit-button`),
      );
      fireEvent.change(
        await screen.findByTestId("NewIndividualAssignmentForm-repoPrefix"),
        { target: { value: "lab01-v2" } },
      );
      fireEvent.click(screen.getByTestId("NewIndividualAssignmentForm-submit"));

      await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
      expect(axiosMock.history.put[0].params.courseId).toBe("7");
      expect(axiosMock.history.put[0].params.assignmentId).toBe(1);
      expect(axiosMock.history.put[0].params.repoPrefix).toBe("lab01-v2");
    });

    test("staff can delete an assignment", async () => {
      setupCourse();
      axiosMock
        .onDelete("/api/assignments/2")
        .reply(200, { message: "Assignment with id 2 deleted" });
      renderPage();
      await openNewAssignmentsTab();

      fireEvent.click(
        await screen.findByTestId(`${tableId}-cell-row-1-col-Delete-button`),
      );
      await screen.findByTestId("ConfirmationModal-base");
      fireEvent.click(screen.getByText("Yes, I'd like to do this"));

      await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
      expect(axiosMock.history.delete[0].params).toEqual({ courseId: "7" });
    });

    test("staff can refresh an assignment, which launches its job", async () => {
      setupCourse();
      axiosMock
        .onPost("/api/assignments/launch")
        .reply(200, newAssignmentsFixtures.savedWithJob);
      renderPage();
      await openNewAssignmentsTab();

      fireEvent.click(
        await screen.findByTestId(`${tableId}-cell-row-2-col-Refresh-button`),
      );

      await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
      expect(axiosMock.history.post[0].url).toBe("/api/assignments/launch");
      expect(axiosMock.history.post[0].params).toEqual({
        courseId: "7",
        assignmentId: 3,
      });
    });

    test("staff can see the log of the job of an assignment, in a modal that follows it, and the link is to the staff address", async () => {
      setupCourse();
      renderPage();
      await openNewAssignmentsTab();

      const link = await screen.findByTestId(
        `${tableId}-cell-row-0-col-lastJobId-link`,
      );
      expect(link).toHaveAttribute("href", "/staff/courses/7/jobs/12/logs");
      fireEvent.click(link);

      expect(await screen.findByText("Job 12 Log")).toBeInTheDocument();
      expect(
        await screen.findByText(/Creating lab01-cgaucho/, { selector: "pre" }),
      ).toHaveTextContent("Creating lab01-cgaucho Done");
      const tailRequests = axiosMock.history.get.filter(
        (request) => request.url === "/api/jobs/course/logs/tail",
      );
      expect(tailRequests[0].params).toEqual({
        courseId: "7",
        jobId: 12,
        afterId: 0,
      });
    });
  });
});
