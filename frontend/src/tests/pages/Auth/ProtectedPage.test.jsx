import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import ProtectedPage from "main/pages/Auth/ProtectedPage";
import { currentUserFixtures } from "fixtures/currentUserFixtures";

const queryClient = new QueryClient();

describe("ProtectedPage tests", () => {
  beforeEach(() => {
    queryClient.clear();
  });
  test("Renders blank on initialData", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <ProtectedPage
            currentUser={{ root: null, initialData: true, rolesList: [] }}
            enforceRole={"ROLE_USER"}
          />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByTestId("LoadingPage-main-div")).toBeInTheDocument();
  });
  test("Renders AccessDenied on missing role", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <ProtectedPage
            currentUser={currentUserFixtures.instructorUser}
            enforceRole={"ROLE_ADMIN"}
          />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("You do not have access to this page.");
  });

  test("Renders ProtectedPage on correct role", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <ProtectedPage
            currentUser={currentUserFixtures.instructorUser}
            enforceRole={"ROLE_INSTRUCTOR"}
            component={<div>Renders successfully.</div>}
          />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Renders successfully.");
  });

  test("Renders PromptSignInPage on no user", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <ProtectedPage
            currentUser={currentUserFixtures.notLoggedIn}
            enforceRole={"ROLE_USER"}
          />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Please sign in before accessing this page.");
  });
});
