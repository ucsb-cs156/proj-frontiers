import { render, waitFor, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router";
import SignInSuccessPage from "main/pages/Auth/SignInSuccessPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import mockConsole from "jest-mock-console";

const mockedNavigate = jest.fn();
const queryClient = new QueryClient();

jest.mock("react-router", () => ({
  ...jest.requireActual("react-router"),
  useNavigate: () => mockedNavigate,
}));
describe("SignInSuccessPage tests", () => {
  beforeEach(() => {
    queryClient.clear();
    sessionStorage.clear();
  });
  test("Page redirects correctly on set value", async () => {
    const restoreConsole = mockConsole();
    sessionStorage.setItem("redirect", "return-url");
    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <SignInSuccessPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => expect(mockedNavigate).toBeCalledWith("return-url"));
    expect(screen.getByText("Redirecting...")).toBeInTheDocument();
    restoreConsole();
  });

  test("Page redirects to / on no value", async () => {
    const restoreConsole = mockConsole();
    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <SignInSuccessPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );
    await waitFor(() => expect(mockedNavigate).toBeCalledWith("/"));
    restoreConsole();
  });
});
