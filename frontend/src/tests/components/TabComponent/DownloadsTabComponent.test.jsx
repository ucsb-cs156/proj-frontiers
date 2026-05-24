import axios from "axios";
import { fireEvent, render, waitFor, screen } from "@testing-library/react";
import DownloadsTabComponent from "main/components/TabComponent/DownloadsTabComponent";
import AxiosMockAdapter from "axios-mock-adapter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { expect, vi } from "vitest";
import coursesFixtures from "fixtures/coursesFixtures";
import * as useBackendModule from "main/utils/useBackend";

const axiosMock = new AxiosMockAdapter(axios);
const mockToast = vi.fn();

const useBackendMutationSpy = vi.spyOn(useBackendModule, "useBackendMutation");

vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

describe("DownloadsTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.resetHistory();
    mockToast.mockClear();
  });

  afterEach(() => {
    useBackendMutationSpy.mockClear();
  });

  test("Downloads tab component and form elements render correctly", async () => {
    axiosMock.onGet("/api/courses/downloadStudentsCSV").reply(200);
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <DownloadsTabComponent
          courseId={coursesFixtures.severalCourses[0].id}
          testIdPrefix="InstructorCourseShowPage"
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("InstructorCourseShowPage-downloadsTab");

    expect(screen.getByText("Course Downloads")).toBeInTheDocument();
    expect(screen.getByTestId("InstructorCourseShowPage-downloads-header")).toBeInTheDocument();
    expect(screen.getByTestId("InstructorCourseShowPage-btn-download-students-csv")).toBeInTheDocument();
  });

  test("Fires submit download handler cleanly on button click", async () => {
    axiosMock.onGet("/api/courses/downloadStudentsCSV").reply(200);
    const client = new QueryClient();
    render(
      <QueryClientProvider client={client}>
        <DownloadsTabComponent 
          courseId={coursesFixtures.severalCourses[0].id} 
          testIdPrefix="InstructorCourseShowPage"
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("InstructorCourseShowPage-btn-download-students-csv");

    const submitButton = screen.getByTestId("InstructorCourseShowPage-btn-download-students-csv");
    fireEvent.click(submitButton);

    await waitFor(() => expect(mockToast).toHaveBeenCalled());
    expect(mockToast).toBeCalledWith("Download successfully initiated.");
  });

  test("useBackendMutation is called with correct structural options", async () => {
    const client = new QueryClient();

    render(
      <QueryClientProvider client={client}>
        <DownloadsTabComponent 
          courseId={coursesFixtures.severalCourses[0].id} 
          testIdPrefix="InstructorCourseShowPage"
        />
      </QueryClientProvider>,
    );

    expect(useBackendMutationSpy).toHaveBeenCalledWith(
      expect.any(Function),
      { onSuccess: expect.any(Function) },
      []
    );

    const extractedFunction = useBackendMutationSpy.mock.calls[0][0];
    const result = extractedFunction();
    expect(result.url).toBe("/api/courses/downloadStudentsCSV");
    expect(result.method).toBe("GET");
  });
});