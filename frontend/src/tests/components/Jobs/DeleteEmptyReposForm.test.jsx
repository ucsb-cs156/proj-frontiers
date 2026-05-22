import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import DeleteEmptyReposForm from "main/components/Jobs/DeleteEmptyRepoForm";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
/* global jest */
import { render, screen, fireEvent, waitFor } from "@testing-library/react";

const mockToast = jest.fn();
jest.mock("react-toastify", () => {
  const originalModule = jest.requireActual("react-toastify");
  return {
    __esModule: true,
    ...originalModule,
    toast: (x) => mockToast(x),
  };
});

describe("DeleteEmptyReposForm tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  const queryClient = new QueryClient();

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    mockToast.mockClear();
  });

  test("renders correctly", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeleteEmptyReposForm courseId={17} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("Delete Empty Repositories")).toBeInTheDocument();
    expect(
      screen.getByTestId("DeleteEmptyReposForm-prefix"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("DeleteEmptyReposForm-submit"),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/delete all repositories in the organization/i),
    ).toBeInTheDocument();
  });

  test("shows error toast if prefix is empty on submit", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeleteEmptyReposForm courseId={17} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const submitButton = screen.getByTestId("DeleteEmptyReposForm-submit");
    fireEvent.click(submitButton);

    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith("Please enter a prefix"),
    );
    // Make sure axios wasn't called
    expect(axiosMock.history.delete.length).toBe(0);
  });

  test("calls API and toasts on successful submit", async () => {
    // Mock the DELETE endpoint
    axiosMock
      .onDelete("/api/repos", { params: { courseId: 17, prefix: "lab01" } })
      .reply(200, {
        message: "Job Launched",
      });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DeleteEmptyReposForm courseId={17} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const prefixInput = screen.getByTestId("DeleteEmptyReposForm-prefix");
    const submitButton = screen.getByTestId("DeleteEmptyReposForm-submit");

    fireEvent.change(prefixInput, { target: { value: "lab01" } });
    fireEvent.click(submitButton);

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));

    expect(axiosMock.history.delete[0].params).toEqual({
      courseId: 17,
      prefix: "lab01",
    });
    expect(mockToast).toHaveBeenCalledWith(
      "Delete empty repos job launched for prefix: lab01",
    );

    // Ensure input was cleared
    expect(prefixInput.value).toBe("");
  });
});
