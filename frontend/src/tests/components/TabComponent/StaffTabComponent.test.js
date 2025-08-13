import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { rosterStudentFixtures } from "fixtures/rosterStudentFixtures";
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import {
  QueryClient,
  QueryClientProvider,
  useQuery,
} from "@tanstack/react-query";
import StaffTabComponent from "main/components/TabComponent/StaffTabComponent";
import userEvent from "@testing-library/user-event";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const testId = "InstructorCourseShowPage";
const mockToast = jest.fn();
jest.mock("react-toastify", () => {
  const originalModule = jest.requireActual("react-toastify");
  return {
    __esModule: true,
    ...originalModule,
    toast: (x) => mockToast(x),
  };
});

const mockedNavigate = jest.fn();
jest.mock("react-router", () => ({
  ...jest.requireActual("react-router"),
  useNavigate: () => mockedNavigate,
}));

const ArbitraryTestQueryComponent = () => {
  const _arbitraryQuery = useQuery({
    queryKey: ["arbitraryQuery"],
    queryFn: () => "banana",
  });
  return <></>;
};

describe("StaffTabComponent Tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });

  test.only("Table Renders", async () => {
    axiosMock
      .onGet("/api/coursestaff/course?courseId=1")
      .reply(200, courseStaffFixtures.threeStaff);

    render(
      <QueryClientProvider client={queryClient}>
        <StaffTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    const rsTestId = "InstructorCourseShowPage-CourseStaffTable";

    await waitFor(() => {
      expect(screen.getByTestId(`${rsTestId}-cell-row-0-col-id`)).toBe;
    });

    expect(
      screen.getByTestId(`${rsTestId}-cell-row-0-col-id`),
    ).toHaveTextContent(courseStaffFixtures.threeStaff[0].id);

    const staffFirstName0 = screen.getByText(
      courseStaffFixtures.threeStaff[0].firstName,
    );
    expect(staffFirstName0).toBeInTheDocument();

    const staffId0 = screen.getByTestId(`${rsTestId}-cell-row-0-col-id`);
    expect(staffId0).toHaveTextContent(courseStaffFixtures.threeStaff[0].id);
  });
  test("Table Renders with no students", async () => {
    axiosMock.onGet("/api/coursestaff/course/7").reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <StaffTabComponent
          courseId={7}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(
          "InstructorCourseShowPage-CourseStaffTable-header-studentId",
        ),
      ).toBeInTheDocument();
    });

    expect(
      screen.queryByTestId(`${testId}-cell-row-0-col-id`),
    ).not.toBeInTheDocument();

    const expectedHeaders = ["Student Id", "First Name", "Last Name", "Email"];
    const expectedFields = ["studentId", "firstName", "lastName", "email"];

    // assert
    expectedHeaders.forEach((headerText, index) => {
      const header = screen.getByTestId(
        `InstructorCourseShowPage-CourseStaffTable-header-${expectedFields[index]}`,
      );
      expect(header).toHaveTextContent(headerText);
    });

    expectedFields.forEach((field) => {
      const fieldElement = screen.queryByTestId(
        `${testId}-cell-row-0-col-${field}`,
      );
      expect(fieldElement).not.toBeInTheDocument();
    });
    expect(screen.queryByTestId(`${testId}-csv-modal`)).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(`${testId}-post-modal`),
    ).not.toBeInTheDocument();
  });

  test("Successfully makes a call to the backend on submit and clears search filter", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    const file = new File(["there"], "egrades.csv", { type: "text/csv" });

    axiosMock
      .onGet("/api/coursestaff/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);

    axiosMock.onPost("/api/coursestaff/upload/csv").reply(200);

    const user = userEvent.setup();
    render(
      <QueryClientProvider client={queryClientSpecific}>
        <ArbitraryTestQueryComponent />
        <StaffTabComponent
          courseId={7}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );
    const openModal = await screen.findByTestId(`${testId}-csv-button`);

    const arbitraryUpdateCount = queryClientSpecific.getQueryState([
      "arbitraryQuery",
    ]).dataUpdateCount;

    const updateCountStudent = queryClientSpecific.getQueryState([
      "/api/coursestaff/course/7",
    ]).dataUpdateCount;

    // Get the search input and set a search term
    const searchInput = screen.getByTestId("InstructorCourseShowPage-search");
    fireEvent.change(searchInput, { target: { value: "test search" } });
    expect(searchInput.value).toBe("test search");

    fireEvent.click(openModal);
    expect(screen.getByTestId(`${testId}-csv-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    const upload = await screen.findByTestId(
      "RosterStudentCSVUploadForm-upload",
    );
    const submitButton = screen.getByTestId(
      "RosterStudentCSVUploadForm-submit",
    );
    await user.upload(upload, file);
    fireEvent.click(submitButton);
    await waitFor(() => {
      expect(axiosMock.history.post[0].params).toEqual({
        courseId: 7,
      });
    });
    expect(axiosMock.history.post[0].data.get("file")).toEqual(file);
    expect(mockToast).toBeCalledWith("Roster successfully updated.");
    expect(
      queryClientSpecific.getQueryState(["arbitraryQuery"]).dataUpdateCount,
    ).toBe(arbitraryUpdateCount);
    expect(
      queryClientSpecific.getQueryState(["/api/coursestaff/course/7"])
        .dataUpdateCount,
    ).toEqual(updateCountStudent + 1);

    // Verify that the search filter is cleared
    await waitFor(() => {
      expect(searchInput.value).toBe("");
    });
    expect(screen.queryByTestId(`${testId}-csv-modal`)).not.toBeInTheDocument();
  });

  test("RosterStudentForm submit works and clears search filter", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    axiosMock
      .onGet("/api/coursestaff/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);

    axiosMock.onPost("/api/coursestaff/post").reply(200);
    render(
      <QueryClientProvider client={queryClientSpecific}>
        <ArbitraryTestQueryComponent />
        <StaffTabComponent
          courseId={7}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    //Great time to check initial values
    expect(
      queryClientSpecific.getQueryData(["/api/coursestaff/course/7"]),
    ).toStrictEqual([]);

    const openModal = await screen.findByTestId(`${testId}-post-button`);
    const arbitraryUpdateCount = queryClientSpecific.getQueryState([
      "arbitraryQuery",
    ]).dataUpdateCount;
    const updateCountStudent = queryClientSpecific.getQueryState([
      "/api/coursestaff/course/7",
    ]).dataUpdateCount;

    fireEvent.click(openModal);
    await screen.findByLabelText("Student Id");
    expect(screen.getByTestId(`${testId}-post-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    // Get the search input and set a search term
    const searchInput = screen.getByTestId("InstructorCourseShowPage-search");
    fireEvent.change(searchInput, { target: { value: "test search" } });
    expect(searchInput.value).toBe("test search");

    expect(screen.queryByText("Cancel")).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Student Id"), {
      target: { value: "123456789" },
    });
    fireEvent.change(screen.getByLabelText("First Name"), {
      target: { value: "Chris" },
    });
    fireEvent.change(screen.getByLabelText("Last Name"), {
      target: { value: "Gaucho" },
    });
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "cgaucho@ucsb.edu" },
    });
    fireEvent.click(screen.getByTestId("RosterStudentForm-submit"));
    await waitFor(() => expect(axiosMock.history.post.length).toEqual(1));
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 7,
      studentId: "123456789",
      firstName: "Chris",
      lastName: "Gaucho",
      email: "cgaucho@ucsb.edu",
    });
    await waitFor(() => expect(mockToast).toBeCalled());
    expect(mockToast).toBeCalledWith("Roster successfully updated.");
    expect(
      queryClientSpecific.getQueryState(["arbitraryQuery"]).dataUpdateCount,
    ).toBe(arbitraryUpdateCount);
    expect(
      queryClientSpecific.getQueryState(["/api/coursestaff/course/7"])
        .dataUpdateCount,
    ).toEqual(updateCountStudent + 1);

    // Verify that the search filter is cleared
    await waitFor(() => {
      expect(searchInput.value).toBe("");
    });
    expect(
      screen.queryByTestId(`${testId}-post-modal`),
    ).not.toBeInTheDocument();
  });

  test("Modals close on close buttons (respectively), download works", async () => {
    const download = jest.fn();
    window.open = (a, b) => download(a, b);
    axiosMock
      .onGet("/api/coursestaff/course/7")
      .reply(200, rosterStudentFixtures.threeStudents);

    render(
      <QueryClientProvider client={queryClient}>
        <ArbitraryTestQueryComponent />
        <StaffTabComponent
          courseId={7}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    const openModalPost = await screen.findByTestId(`${testId}-post-button`);
    fireEvent.click(openModalPost);
    let closeButton = await screen.findByRole("button", { name: "Close" });
    fireEvent.click(closeButton);
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-post-modal`),
      ).not.toBeInTheDocument(),
    );
    const openModalCsv = await screen.findByTestId(`${testId}-csv-button`);
    fireEvent.click(openModalCsv);
    closeButton = await screen.findByRole("button", { name: "Close" });
    fireEvent.click(closeButton);
    await waitFor(() =>
      expect(
        screen.queryByTestId(`${testId}-csv-modal`),
      ).not.toBeInTheDocument(),
    );
    fireEvent.click(screen.getByText("Download Student CSV"));
    await waitFor(() => expect(download).toBeCalled());
    expect(download).toBeCalledWith(
      "/api/csv/coursestaff?courseId=7",
      "_blank",
    );
  });

  describe("Search filter works correctly", () => {
    const testId = "InstructorCourseShowPage";
    const rsTestId = "InstructorCourseShowPage-CourseStaffTable";
    const studentList = [
      ...rosterStudentFixtures.studentsWithEachStatus,
      {
        id: 7,
        studentId: "A626737",
        firstName: "Fake",
        lastName: "Name",
        email: "fakename@ucsb.edu",
        githubLogin: "DifferingGitHub",
        orgStatus: "JOINCOURSE",
      },
    ];
    beforeEach(() => {
      axiosMock.onGet("/api/coursestaff/course/1").reply(200, studentList);
    });

    test("PLaceholder, initial check", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <StaffTabComponent
            courseId={1}
            testIdPrefix={testId}
            currentUser={currentUserFixtures.instructorUser}
          />
        </QueryClientProvider>,
      );
      await waitFor(() => {
        expect(
          screen.getByTestId(`${rsTestId}-cell-row-0-col-id`),
        ).toBeInTheDocument();
      });

      const searchInput = screen.getByTestId(`${testId}-search`);
      expect(searchInput).toBeInTheDocument();
      expect(searchInput).toHaveAttribute(
        "placeholder",
        "Search by name, email, student ID, or Github Login",
      );
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-firstName`),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-1-col-firstName`),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-2-col-firstName`),
      ).toBeInTheDocument();
    });

    test("First Name, Email", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <StaffTabComponent
            courseId={1}
            testIdPrefix={testId}
            currentUser={currentUserFixtures.instructorUser}
          />
        </QueryClientProvider>,
      );

      await waitFor(() => {
        expect(
          screen.getByTestId(`${rsTestId}-cell-row-0-col-id`),
        ).toBeInTheDocument();
      });

      // Verify search input is rendered
      const searchInput = screen.getByTestId(`${testId}-search`);

      const fullNameStudent = rosterStudentFixtures.studentsWithEachStatus[2]; // Emma Watson
      fireEvent.change(searchInput, {
        target: {
          value:
            `${fullNameStudent.firstName} ${fullNameStudent.lastName}`.toUpperCase(),
        },
      });

      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-firstName`),
      ).toHaveTextContent(fullNameStudent.firstName);
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-lastName`),
      ).toHaveTextContent(fullNameStudent.lastName);
      expect(
        screen.queryByTestId(`${rsTestId}-cell-row-1-col-firstName`),
      ).not.toBeInTheDocument();

      fireEvent.change(searchInput, { target: { value: "" } });

      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-firstName`),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-1-col-firstName`),
      ).toBeInTheDocument();
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-2-col-firstName`),
      ).toBeInTheDocument();

      fireEvent.change(searchInput, {
        target: {
          value:
            rosterStudentFixtures.studentsWithEachStatus[1].email.toUpperCase(),
        },
      });

      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-email`),
      ).toHaveTextContent(
        rosterStudentFixtures.studentsWithEachStatus[1].email,
      );
      expect(
        screen.queryByTestId(`${rsTestId}-cell-row-1-col-email`),
      ).not.toBeInTheDocument();
    });

    test("GitHub Login, Student ID", async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <StaffTabComponent
            courseId={1}
            testIdPrefix={testId}
            currentUser={currentUserFixtures.instructorUser}
          />
        </QueryClientProvider>,
      );
      await waitFor(() => {
        expect(
          screen.getByTestId(`${rsTestId}-cell-row-0-col-id`),
        ).toBeInTheDocument();
      });

      const searchInput = screen.getByTestId(`${testId}-search`);
      const studentWithGithub = studentList[6].githubLogin;
      fireEvent.change(searchInput, {
        target: { value: studentWithGithub.toUpperCase() },
      });

      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-githubLogin`),
      ).toHaveTextContent(studentWithGithub);
      expect(
        screen.queryByTestId(`${rsTestId}-cell-row-1-col-firstName`),
      ).not.toBeInTheDocument();

      fireEvent.change(searchInput, { target: { value: "" } });

      fireEvent.change(searchInput, {
        target: {
          value:
            rosterStudentFixtures.studentsWithEachStatus[1].studentId.toUpperCase(),
        },
      });

      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-studentId`),
      ).toHaveTextContent(
        rosterStudentFixtures.studentsWithEachStatus[1].studentId,
      );
      expect(
        screen.queryByTestId(`${rsTestId}-cell-row-1-col-studentId`),
      ).not.toBeInTheDocument();
    });
  });
});
