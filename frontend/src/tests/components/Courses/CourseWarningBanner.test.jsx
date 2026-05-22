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
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: true,
      defaultBasePermission: "none",
    });
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
        placeholderData: { showOrganizationAgeWarning: false },
        staleTime: "static",
      },
    );
  });
  test("Does not render banner on false", async () => {
    vi.spyOn(useBackend, "useBackend");
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: false,
      defaultBasePermission: "none",
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
    expect(
      screen.queryByText(/default base permission/i),
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
    expect(
      screen.queryByText(/default base permission/i),
    ).not.toBeInTheDocument();
  });

  test("renders permission warning with link when value is read", async () => {
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: false,
      defaultBasePermission: "read",
    });
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner courseId={1} orgName="ucsb-cs156-s25" />
      </QueryClientProvider>,
    );
    await screen.findByText(/default base permission for this organization/i);
    expect(screen.getByText(/"Read"/)).toBeInTheDocument();
    const link = screen.getByRole("link", {
      name: /change this in github settings/i,
    });
    expect(link).toHaveAttribute(
      "href",
      "https://github.com/organizations/ucsb-cs156-s25/settings/member_privileges",
    );
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });

  test("renders permission warning when value is write", async () => {
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: false,
      defaultBasePermission: "write",
    });
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner courseId={1} orgName="ucsb-cs156-s25" />
      </QueryClientProvider>,
    );
    await screen.findByText(/"Write"/);
  });

  test("renders permission warning when value is admin", async () => {
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: false,
      defaultBasePermission: "admin",
    });
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner courseId={1} orgName="ucsb-cs156-s25" />
      </QueryClientProvider>,
    );
    await screen.findByText(/"Admin"/);
  });

  test("does not render permission warning when value is none", async () => {
    vi.spyOn(useBackend, "useBackend");
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: false,
      defaultBasePermission: "none",
    });
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner courseId={1} orgName="ucsb-cs156-s25" />
      </QueryClientProvider>,
    );
    await waitFor(() => {
      expect(useBackend.useBackend).toBeCalled();
    });
    expect(
      screen.queryByText(/default base permission for this organization/i),
    ).not.toBeInTheDocument();
  });

  test("does not render permission warning when value is null (no org)", async () => {
    vi.spyOn(useBackend, "useBackend");
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: false,
      defaultBasePermission: "null",
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
      screen.queryByText(/default base permission for this organization/i),
    ).not.toBeInTheDocument();
  });

  test("renders both warnings when both flags set", async () => {
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: true,
      defaultBasePermission: "read",
    });
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner courseId={1} orgName="ucsb-cs156-s25" />
      </QueryClientProvider>,
    );
    await screen.findByText(/This GitHub Organization/i);
    expect(
      screen.getByText(/default base permission for this organization/i),
    ).toBeInTheDocument();
  });

  test("omits link when orgName is not provided", async () => {
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: false,
      defaultBasePermission: "read",
    });
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner courseId={1} />
      </QueryClientProvider>,
    );
    await screen.findByText(/default base permission for this organization/i);
    expect(
      screen.queryByRole("link", { name: /change this in github settings/i }),
    ).not.toBeInTheDocument();
  });
});
