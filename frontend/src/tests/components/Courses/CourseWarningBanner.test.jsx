import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { CourseWarningBanner } from "main/components/Courses/CourseWarningBanner";
import * as useBackend from "main/utils/useBackend";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
describe("CourseWarningBanner tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
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
      screen.getByTestId("CourseWarningBanner-defaultBasePermission").textContent,
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
});
