import { BrowserRouter } from "react-router";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import NotFoundPage from "main/pages/Auth/NotFoundPage";

const queryClient = new QueryClient();
test("Not Found Page static checks", async () => {
  render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <NotFoundPage />
      </BrowserRouter>
    </QueryClientProvider>,
  );

  await screen.findByText(/Page Not Found/);
  expect(screen.getByText("Let's get you back on track.")).toBeInTheDocument();
  expect(screen.getByText("Click to Return Home")).toBeInTheDocument();
});
