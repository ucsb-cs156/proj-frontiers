import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import DeleteEmptyRepoForm from "main/components/Jobs/DeleteEmptyRepoForm";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { vi } from "vitest";

const { mockToast, mockToastError } = vi.hoisted(() => {
  return {
    mockToast: vi.fn(),
    mockToastError: vi.fn(),
  };
});

vi.mock("react-toastify", async (importOriginal) => {
  const originalModule = await importOriginal();
  return {
    ...originalModule,
    toast: Object.assign(mockToast, {
      error: mockToastError,
    }),
  };
});

describe("DeleteEmptyRepoForm tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  const queryClient = new QueryClient();

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    mockToast.mockClear();
    mockToastError.mockClear();
  });

  test("renders correctly and checks initial state", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeleteEmptyRepoForm courseId={17} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("Delete Empty Repositories")).toBeInTheDocument();

    const prefixInput = screen.getByTestId("DeleteEmptyReposForm-prefix");
    expect(prefixInput).toBeInTheDocument();
    expect(prefixInput).toHaveValue("");

    const submitButton = screen.getByTestId("DeleteEmptyReposForm-submit");
    expect(submitButton).toBeInTheDocument();
    expect(submitButton).toHaveTextContent("Delete Empty Matching Repos");
  });

  test("shows error toast if prefix is empty on submit", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeleteEmptyRepoForm courseId={17} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const submitButton = screen.getByTestId("DeleteEmptyReposForm-submit");
    fireEvent.click(submitButton);

    await waitFor(() =>
      expect(mockToastError).toHaveBeenCalledWith("Please enter a prefix"),
    );
    expect(axiosMock.history.delete.length).toBe(0);
  });

  test("calls API, toasts success, and clears input on valid submit", async () => {
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");

    axiosMock.onDelete("/api/repos").reply(200, { message: "Job Launched" });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeleteEmptyRepoForm courseId={17} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const prefixInput = screen.getByTestId("DeleteEmptyReposForm-prefix");
    const submitButton = screen.getByTestId("DeleteEmptyReposForm-submit");

    fireEvent.change(prefixInput, { target: { value: "lab01" } });
    expect(prefixInput).toHaveValue("lab01");

    fireEvent.click(submitButton);

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));

    expect(axiosMock.history.delete[0].url).toBe("/api/repos");
    expect(axiosMock.history.delete[0].method).toBe("delete");
    expect(axiosMock.history.delete[0].params).toEqual({
      courseId: 17,
      prefix: "lab01",
    });

    expect(mockToast).toHaveBeenCalledWith(
      "Delete empty repos job launched for prefix: lab01",
    );
    expect(prefixInput).toHaveValue("");

    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["/api/repos"] });
  });

  test("handles API errors (with response data) correctly", async () => {
    axiosMock
      .onDelete("/api/repos")
      .reply(400, { message: "Course not found" });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeleteEmptyRepoForm courseId={17} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const prefixInput = screen.getByTestId("DeleteEmptyReposForm-prefix");
    const submitButton = screen.getByTestId("DeleteEmptyReposForm-submit");

    fireEvent.change(prefixInput, { target: { value: "lab01" } });
    fireEvent.click(submitButton);

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));

    expect(mockToastError).toHaveBeenCalledWith(
      "Error starting job: Course not found",
    );
  });

  test("handles API errors (with null response data) correctly", async () => {
    axiosMock.onDelete("/api/repos").reply(400, null);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeleteEmptyRepoForm courseId={17} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const prefixInput = screen.getByTestId("DeleteEmptyReposForm-prefix");
    const submitButton = screen.getByTestId("DeleteEmptyReposForm-submit");

    fireEvent.change(prefixInput, { target: { value: "lab01" } });
    fireEvent.click(submitButton);

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));

    expect(mockToastError).toHaveBeenCalledWith(
      expect.stringContaining(
        "Error starting job: Request failed with status code 400",
      ),
    );
  });

  test("handles API errors (without response data fallback) correctly", async () => {
    axiosMock.onDelete("/api/repos").networkError();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeleteEmptyRepoForm courseId={17} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const prefixInput = screen.getByTestId("DeleteEmptyReposForm-prefix");
    const submitButton = screen.getByTestId("DeleteEmptyReposForm-submit");

    fireEvent.change(prefixInput, { target: { value: "lab01" } });
    fireEvent.click(submitButton);

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));

    expect(mockToastError).toHaveBeenCalledWith(
      "Error starting job: Network Error",
    );
  });

  test("button shows Launching... and is disabled while mutating", async () => {
    let resolveApi;
    const controlledPromise = new Promise((resolve) => {
      resolveApi = resolve;
    });

    axiosMock.onDelete("/api/repos").reply(() => controlledPromise);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeleteEmptyRepoForm courseId={17} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const prefixInput = screen.getByTestId("DeleteEmptyReposForm-prefix");
    const submitButton = screen.getByTestId("DeleteEmptyReposForm-submit");

    fireEvent.change(prefixInput, { target: { value: "lab01" } });
    fireEvent.click(submitButton);

    expect(await screen.findByText("Launching...")).toBeInTheDocument();
    expect(submitButton).toBeDisabled();

    resolveApi([200, { message: "Job Launched" }]);

    await waitFor(() =>
      expect(screen.queryByText("Launching...")).not.toBeInTheDocument(),
    );
  });
});
