import { render, screen } from "@testing-library/react";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
const queryClient = new QueryClient();
describe("BasicLayout Test", () => {
  test("renders with correct classes", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <BasicLayout enableBootstrap={true} />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    const container = screen.getByTestId("BasicLayout-container");
    expect(container).toBeInTheDocument();
    expect(container).toHaveClass("pt-4 flex-grow-1 d-flex flex-column");
  });
  test("renders without bootstrap classes", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <BasicLayout />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    const container = screen.getByTestId("BasicLayout-container");
    expect(container).toBeInTheDocument();
    expect(container).not.toHaveClass("d-flex flex-column");
  });
});
