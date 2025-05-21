import { render, screen } from "@testing-library/react";
import { roleEmailFixtures } from "fixtures/roleEmailFixtures";
import { BrowserRouter as Router } from "react-router-dom";
import RoleEmailTable from "main/components/Users/RoleEmailTable";

import { QueryClient, QueryClientProvider } from "react-query";
const queryClient = new QueryClient();

describe("RoleEmailTable tests", () => {
  test("renders without crashing for empty table", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RoleEmailTable users={[]}/>
        </Router>
      </QueryClientProvider>,
    );
  });

  test("renders without crashing for three users", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RoleEmailTable users={roleEmailFixtures.threeRoleEmails}/>
        </Router>
      </QueryClientProvider>,
    );
  });

  test("Has the expected column headers and content", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RoleEmailTable users={roleEmailFixtures.threeRoleEmails}/>
        </Router>
      </QueryClientProvider>,
    );

    const expectedHeaders = ["Email"];
    const expectedFields = ["email"];
    const testId = "RoleEmailTable";

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    expectedFields.forEach((field) => {
      const header = screen.getByTestId(`${testId}-cell-row-0-col-${field}`);
      expect(header).toBeInTheDocument();
    });

    expect(screen.getByTestId(`${testId}-cell-row-0-col-email`)).toHaveTextContent(
      "joegaucho@ucsb.edu",
    );
  });
});
