import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import {
  CourseWarningBanner,
  GITHUB_EDUCATION_TEACHER_URL,
} from "main/components/Courses/CourseWarningBanner";
import * as useBackend from "main/utils/useBackend";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const mockToast = vi.fn();

vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

describe("CourseWarningBanner tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    mockToast.mockClear();
  });

  test("renders warning banner on warning return", async () => {
    vi.spyOn(useBackend, "useBackend");
    axiosMock
      .onGet("/api/courses/warnings/1")
      .reply(200, { showOrganizationAgeWarning: true });
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner courseId={1} />
      </QueryClientProvider>,
    );
    await screen.findByText(/This GitHub Organization/i);
    expect(useBackend.useBackend).toHaveBeenCalledWith(
      [`/api/courses/warnings/1`],
      {
        method: "GET",
        url: `/api/courses/warnings/1`,
      },
      undefined,
      true,
      {
        placeholderData: {
          showOrganizationAgeWarning: false,
          showDefaultBasePermissions: false,
          showFreePlanWarning: false,
        },
        staleTime: "static",
      },
    );
  });
  test("Does not render banner on false", async () => {
    vi.spyOn(useBackend, "useBackend");
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: false,
    });
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner courseId={1} />
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(useBackend.useBackend).toBeCalled();
    });
    expect(
      screen.queryByText(/This GitHub Organization/i),
    ).not.toBeInTheDocument();
  });
  test("No misbehavior on empty return", async () => {
    vi.spyOn(useBackend, "useBackend");
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {});
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner courseId={1} />
      </QueryClientProvider>,
    );
    await waitFor(() => {
      expect(useBackend.useBackend).toBeCalled();
    });
    expect(
      screen.queryByText(/This GitHub Organization/i),
    ).not.toBeInTheDocument();
  });

  test("renders default base permission warning with settings link", async () => {
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: false,
      showDefaultBasePermissions: true,
    });
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner courseId={1} orgName="ucsb-cs156-s26" />
      </QueryClientProvider>,
    );
    await screen.findByTestId("CourseWarningBanner-defaultBasePermission");
    expect(
      screen.getByText(/Default Base Permission is not the recommended value/i),
    ).toBeInTheDocument();
    const link = screen.getByTestId(
      "CourseWarningBanner-defaultBasePermission-link",
    );
    expect(link).toHaveAttribute(
      "href",
      "https://github.com/organizations/ucsb-cs156-s26/settings/member_privileges",
    );
    expect(link).toHaveTextContent("You can change that setting here");
    expect(
      screen.getByTestId("CourseWarningBanner-defaultBasePermission")
        .textContent,
    ).toContain("private repos. You can change");
  });

  test("does not render default base permission warning when showDefaultBasePermissions is false", async () => {
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showDefaultBasePermissions: false,
    });
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner courseId={1} orgName="ucsb-cs156-s26" />
      </QueryClientProvider>,
    );
    await waitFor(() => {
      expect(useBackend.useBackend).toBeCalled();
    });
    expect(
      screen.queryByTestId("CourseWarningBanner-defaultBasePermission"),
    ).not.toBeInTheDocument();
  });

  test("does not render default base permission warning when hideBasePermissionWarning is true", async () => {
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showDefaultBasePermissions: true,
    });
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner
          courseId={1}
          orgName="ucsb-cs156-s26"
          hideBasePermissionWarning={true}
        />
      </QueryClientProvider>,
    );
    await waitFor(() => {
      expect(useBackend.useBackend).toBeCalled();
    });
    expect(
      screen.queryByTestId("CourseWarningBanner-defaultBasePermission"),
    ).not.toBeInTheDocument();
  });

  test("does not render default base permission warning without orgName", async () => {
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showDefaultBasePermissions: true,
    });
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner courseId={1} />
      </QueryClientProvider>,
    );
    await waitFor(() => {
      expect(useBackend.useBackend).toBeCalled();
    });
    expect(
      screen.queryByTestId("CourseWarningBanner-defaultBasePermission"),
    ).not.toBeInTheDocument();
  });

  describe("free plan warning", () => {
    test("renders free plan warning with upgrade link and signed commits note", async () => {
      axiosMock.onGet("/api/courses/warnings/1").reply(200, {
        showOrganizationAgeWarning: false,
        showDefaultBasePermissions: false,
        showFreePlanWarning: true,
      });
      render(
        <QueryClientProvider client={queryClient}>
          <CourseWarningBanner courseId={1} orgName="ucsb-cs156-s26" />
        </QueryClientProvider>,
      );
      const alert = await screen.findByTestId("CourseWarningBanner-freePlan");
      expect(alert).toHaveClass("alert-warning");
      expect(alert.textContent).toContain(
        "Warning: this GitHub organization is on the Free plan.",
      );
      expect(alert.textContent).toContain(
        'the "Require Signed Commits" option for assignments cannot be applied to private repositories',
      );
      expect(alert.textContent).toContain(
        "Verified educators can upgrade the organization at no cost through GitHub Education for Teachers.",
      );

      const link = screen.getByTestId("CourseWarningBanner-freePlan-link");
      expect(link).toHaveAttribute(
        "href",
        "https://education.github.com/globalcampus/teacher",
      );
      expect(GITHUB_EDUCATION_TEACHER_URL).toBe(
        "https://education.github.com/globalcampus/teacher",
      );
      expect(link).toHaveAttribute("target", "_blank");
      expect(link).toHaveAttribute("rel", "noopener noreferrer");
      expect(link).toHaveTextContent("GitHub Education for Teachers");

      const dismiss = screen.getByTestId(
        "CourseWarningBanner-freePlan-dismiss",
      );
      expect(dismiss).toHaveTextContent("Dismiss");
      expect(dismiss).toHaveClass("btn-sm");
      expect(dismiss).toHaveClass("btn-outline-secondary");
      expect(dismiss).toHaveClass("ms-2");
    });

    test("renders free plan warning even without orgName", async () => {
      axiosMock.onGet("/api/courses/warnings/1").reply(200, {
        showFreePlanWarning: true,
      });
      render(
        <QueryClientProvider client={queryClient}>
          <CourseWarningBanner courseId={1} />
        </QueryClientProvider>,
      );
      await screen.findByTestId("CourseWarningBanner-freePlan");
    });

    test("does not render free plan warning when showFreePlanWarning is false", async () => {
      vi.spyOn(useBackend, "useBackend");
      axiosMock.onGet("/api/courses/warnings/1").reply(200, {
        showFreePlanWarning: false,
      });
      render(
        <QueryClientProvider client={queryClient}>
          <CourseWarningBanner courseId={1} orgName="ucsb-cs156-s26" />
        </QueryClientProvider>,
      );
      await waitFor(() => {
        expect(useBackend.useBackend).toBeCalled();
      });
      expect(
        screen.queryByTestId("CourseWarningBanner-freePlan"),
      ).not.toBeInTheDocument();
    });

    test("does not render free plan warning when hideFreePlanWarning is true", async () => {
      vi.spyOn(useBackend, "useBackend");
      axiosMock.onGet("/api/courses/warnings/1").reply(200, {
        showFreePlanWarning: true,
      });
      render(
        <QueryClientProvider client={queryClient}>
          <CourseWarningBanner
            courseId={1}
            orgName="ucsb-cs156-s26"
            hideFreePlanWarning={true}
          />
        </QueryClientProvider>,
      );
      await waitFor(() => {
        expect(useBackend.useBackend).toBeCalled();
      });
      expect(
        screen.queryByTestId("CourseWarningBanner-freePlan"),
      ).not.toBeInTheDocument();
    });

    test("dismiss posts to the backend, hides the warning, and refreshes the course", async () => {
      axiosMock.onGet("/api/courses/warnings/1").reply(200, {
        showFreePlanWarning: true,
      });
      axiosMock
        .onPost("/api/courses/warnings/hideFreePlanWarning/1")
        .reply(200, {
          message: "hideFreePlanWarning set to true for course with id 1",
        });
      const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");

      render(
        <QueryClientProvider client={queryClient}>
          <CourseWarningBanner courseId={1} orgName="ucsb-cs156-s26" />
        </QueryClientProvider>,
      );
      await screen.findByTestId("CourseWarningBanner-freePlan");

      fireEvent.click(
        screen.getByTestId("CourseWarningBanner-freePlan-dismiss"),
      );

      await waitFor(() => {
        expect(axiosMock.history.post.length).toBe(1);
      });
      expect(axiosMock.history.post[0].url).toBe(
        "/api/courses/warnings/hideFreePlanWarning/1",
      );
      await waitFor(() => {
        expect(mockToast).toHaveBeenCalledWith(
          "Free plan warning dismissed for this course",
        );
      });
      expect(
        screen.queryByTestId("CourseWarningBanner-freePlan"),
      ).not.toBeInTheDocument();
      await waitFor(() => {
        expect(invalidateSpy).toHaveBeenCalledWith({
          queryKey: ["/api/courses/1"],
        });
      });
    });

    test("warning stays visible when dismissing fails", async () => {
      axiosMock.onGet("/api/courses/warnings/1").reply(200, {
        showFreePlanWarning: true,
      });
      axiosMock
        .onPost("/api/courses/warnings/hideFreePlanWarning/1")
        .reply(500, {});

      render(
        <QueryClientProvider client={queryClient}>
          <CourseWarningBanner courseId={1} orgName="ucsb-cs156-s26" />
        </QueryClientProvider>,
      );
      await screen.findByTestId("CourseWarningBanner-freePlan");

      fireEvent.click(
        screen.getByTestId("CourseWarningBanner-freePlan-dismiss"),
      );

      await waitFor(() => {
        expect(mockToast).toHaveBeenCalled();
      });
      expect(mockToast).not.toHaveBeenCalledWith(
        "Free plan warning dismissed for this course",
      );
      expect(
        screen.getByTestId("CourseWarningBanner-freePlan"),
      ).toBeInTheDocument();
    });
  });
});
