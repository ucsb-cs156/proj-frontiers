import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { CourseWarningBanner } from "main/components/Courses/CourseWarningBanner";
import * as useBackendModule from "main/utils/useBackend";
import { useBackend } from "main/utils/useBackend";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();

function BannerWithProbe({ courseId, orgName }) {
  const { data } = useBackend([`/api/courses/warnings/${courseId}`], {
    method: "GET",
    url: `/api/courses/warnings/${courseId}`,
  });
  return (
    <>
      <div data-testid="probe">
        {data?.defaultBasePermission ?? "loading"}|
        {String(data?.showOrganizationAgeWarning ?? "loading")}
      </div>
      <CourseWarningBanner courseId={courseId} orgName={orgName} />
    </>
  );
}

describe("CourseWarningBanner tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
  });

  test("renders warning banner on warning return", async () => {
    vi.spyOn(useBackendModule, "useBackend");
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
    expect(useBackendModule.useBackend).toHaveBeenCalledWith(
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
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: false,
      defaultBasePermission: "none",
    });
    render(
      <QueryClientProvider client={queryClient}>
        <BannerWithProbe courseId={1} />
      </QueryClientProvider>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("probe")).toHaveTextContent("none|false");
    });
    expect(
      screen.queryByText(/This GitHub Organization/i),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText(/not the recommended value of None/i),
    ).not.toBeInTheDocument();
  });

  test("No misbehavior on empty return", async () => {
    vi.spyOn(useBackendModule, "useBackend");
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {});
    render(
      <QueryClientProvider client={queryClient}>
        <CourseWarningBanner courseId={1} />
      </QueryClientProvider>,
    );
    await waitFor(() => {
      expect(useBackendModule.useBackend).toBeCalled();
    });
    expect(
      screen.queryByText(/This GitHub Organization/i),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText(/not the recommended value of None/i),
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
    await screen.findByText(/not the recommended value of None/i);
    const link = screen.getByRole("link", {
      name: /you can change that setting here/i,
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
    await screen.findByText(/not the recommended value of None/i);
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
    await screen.findByText(/not the recommended value of None/i);
  });

  test("does not render permission warning when value is none", async () => {
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: false,
      defaultBasePermission: "none",
    });
    render(
      <QueryClientProvider client={queryClient}>
        <BannerWithProbe courseId={1} orgName="ucsb-cs156-s25" />
      </QueryClientProvider>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("probe")).toHaveTextContent("none|false");
    });
    expect(
      screen.queryByText(/not the recommended value of None/i),
    ).not.toBeInTheDocument();
  });

  test("does not render permission warning when value is null (no org)", async () => {
    axiosMock.onGet("/api/courses/warnings/1").reply(200, {
      showOrganizationAgeWarning: false,
      defaultBasePermission: "null",
    });
    render(
      <QueryClientProvider client={queryClient}>
        <BannerWithProbe courseId={1} />
      </QueryClientProvider>,
    );
    await waitFor(() => {
      expect(screen.getByTestId("probe")).toHaveTextContent("null|false");
    });
    expect(
      screen.queryByText(/not the recommended value of None/i),
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
      screen.getByText(/not the recommended value of None/i),
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
    await screen.findByText(/not the recommended value of None/i);
    expect(
      screen.queryByRole("link", {
        name: /you can change that setting here/i,
      }),
    ).not.toBeInTheDocument();
  });
});
