import { BrowserRouter } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import AccessDeniedPage from "main/pages/Auth/AccessDeniedPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const queryClient = new QueryClient();
test("Access Denied Page static checks", async () => {
  render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AccessDeniedPage />
      </BrowserRouter>
    </QueryClientProvider>,
  );

  await screen.findByText(/You do not have access to this page/);
  expect(screen.getByText("Return")).toBeInTheDocument();
});
