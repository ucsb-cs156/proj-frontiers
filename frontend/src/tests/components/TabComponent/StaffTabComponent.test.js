import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import {
  QueryClient,
  QueryClientProvider,
  useQuery,
} from "@tanstack/react-query";
import StaffTabComponent from "main/components/TabComponent/StaffTabComponent";
import { currentUserFixtures } from "fixtures/currentUserFixtures";
import { courseStaffFixtures } from "fixtures/courseStaffFixtures";

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

let axiosMock;

describe("StaffTabComponent Tests", () => {
  beforeEach(() => {
    axiosMock = new AxiosMockAdapter(axios);
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });

  afterEach(() => {
    axiosMock.restore();
  });

  test("Table Renders", async () => {
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
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-id`),
      ).toBeInTheDocument();
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
    axiosMock.onGet("/api/coursestaff/course?courseId=7").reply(200, []);

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
          "InstructorCourseShowPage-CourseStaffTable-header-id",
        ),
      ).toBeInTheDocument();
    });

    expect(
      screen.queryByTestId(`${testId}-cell-row-0-col-id`),
    ).not.toBeInTheDocument();

    const expectedHeaders = ["id", "First Name", "Last Name", "Email"];
    const expectedFields = ["id", "firstName", "lastName", "email"];

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

  test("StaffForm submit works and clears search filter", async () => {
    const queryClientSpecific = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: Infinity,
        },
      },
    });
    axiosMock
      .onGet("/api/coursestaff/course?courseId=7")
      .reply(200, courseStaffFixtures.threeStaff);

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
      queryClientSpecific.getQueryData(["/api/coursestaff/course?courseId=7"]),
    ).toStrictEqual([]);

    const openModal = await screen.findByTestId(`${testId}-post-button`);
    const arbitraryUpdateCount = queryClientSpecific.getQueryState([
      "arbitraryQuery",
    ]).dataUpdateCount;
    const updateCountStudent = queryClientSpecific.getQueryState([
      "/api/coursestaff/course?courseId=7",
    ]).dataUpdateCount;

    fireEvent.click(openModal);
    await screen.findByLabelText("First Name");
    expect(screen.getByTestId(`${testId}-post-modal`)).toHaveClass(
      "modal-dialog modal-dialog-centered",
    );

    // Get the search input and set a search term
    const searchInput = screen.getByTestId("InstructorCourseShowPage-search");
    fireEvent.change(searchInput, { target: { value: "test search" } });
    expect(searchInput.value).toBe("test search");

    expect(screen.queryByText("Cancel")).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("First Name"), {
      target: { value: "Chris" },
    });
    fireEvent.change(screen.getByLabelText("Last Name"), {
      target: { value: "Gaucho" },
    });
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "cgaucho@ucsb.edu" },
    });
    fireEvent.click(screen.getByTestId("CourseStaffForm-submit"));
    await waitFor(() => expect(axiosMock.history.post.length).toEqual(1));
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 7,
      firstName: "Chris",
      lastName: "Gaucho",
      email: "cgaucho@ucsb.edu",
    });
    await waitFor(() => expect(mockToast).toBeCalled());
    expect(mockToast).toBeCalledWith("Staff roster successfully updated.");
    expect(
      queryClientSpecific.getQueryState(["arbitraryQuery"]).dataUpdateCount,
    ).toBe(arbitraryUpdateCount);
    expect(
      queryClientSpecific.getQueryState(["/api/coursestaff/course?courseId=7"])
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

  describe("Search filter works correctly", () => {
    const testId = "InstructorCourseShowPage";
    const rsTestId = "InstructorCourseShowPage-CourseStaffTable";
    const staffList = [
      ...courseStaffFixtures.staffWithEachStatus,
      {
        id: 7,
        firstName: "Fake",
        lastName: "Name",
        email: "fakename@ucsb.edu",
        githubLogin: "DifferingGitHub",
        orgStatus: "JOINCOURSE",
      },
    ];

    beforeEach(() => {
      axiosMock = new AxiosMockAdapter(axios);
      axiosMock.reset();
      axiosMock.resetHistory();
      axiosMock
        .onGet("/api/coursestaff/course?courseId=1")
        .reply(200, staffList);
      queryClient.clear();
    });

    test("Placeholder, initial check", async () => {
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
        "Search by name, email, or Github Login",
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

      const fullNameStaff = courseStaffFixtures.staffWithEachStatus[2]; // Emma Watson
      fireEvent.change(searchInput, {
        target: {
          value:
            `${fullNameStaff.firstName} ${fullNameStaff.lastName}`.toUpperCase(),
        },
      });

      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-firstName`),
      ).toHaveTextContent(fullNameStaff.firstName);
      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-lastName`),
      ).toHaveTextContent(fullNameStaff.lastName);
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
          value: courseStaffFixtures.staffWithEachStatus[1].email.toUpperCase(),
        },
      });

      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-email`),
      ).toHaveTextContent(courseStaffFixtures.staffWithEachStatus[1].email);
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
      const staffWithGithub = staffList[6].githubLogin;
      fireEvent.change(searchInput, {
        target: { value: staffWithGithub.toUpperCase() },
      });

      expect(
        screen.getByTestId(`${rsTestId}-cell-row-0-col-githubLogin`),
      ).toHaveTextContent(staffWithGithub);
      expect(
        screen.queryByTestId(`${rsTestId}-cell-row-1-col-firstName`),
      ).not.toBeInTheDocument();
    });
  });

  test("for coming soon tooltip on disabled upload CSV button", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <StaffTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    // Wait for table to render
    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-CourseStaffTable`),
      ).toBeInTheDocument();
    });

    // Find the disabled buttons
    const uploadCsvButton = screen.getByTestId(`${testId}-csv-button`);
    expect(uploadCsvButton).toBeDisabled();

    // Simulate mouse over to trigger tooltip
    fireEvent.mouseOver(uploadCsvButton);

    // Tooltip should appear
    await waitFor(() => {
      expect(screen.getByText("Coming Soon")).toBeInTheDocument();
    });
  });

  test("for coming soon tooltip on disabled download CSV button", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <StaffTabComponent
          courseId={1}
          testIdPrefix={testId}
          currentUser={currentUserFixtures.instructorUser}
        />
      </QueryClientProvider>,
    );

    // Wait for table to render
    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-CourseStaffTable`),
      ).toBeInTheDocument();
    });

    // Download CSV button (no testId, but can find by text)
    const downloadCsvButton = screen.getByText("Download Staff CSV");
    expect(downloadCsvButton).toBeDisabled();

    fireEvent.mouseOver(downloadCsvButton);

    await waitFor(() => {
      expect(screen.getByText("Coming Soon")).toBeInTheDocument();
    });
  });

  test("Create Staff Member Modals closes on close button", async () => {
    const download = jest.fn();
    window.open = (a, b) => download(a, b);
    axiosMock
      .onGet("/api/coursestaff/course?courseId=1")
      .reply(200, courseStaffFixtures.threeStaff);

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
  });
});
