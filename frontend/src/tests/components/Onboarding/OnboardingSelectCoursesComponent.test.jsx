import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import coursesFixtures from "fixtures/coursesFixtures";
import { render, screen } from "@testing-library/react";
import OnboardingSelectCoursesComponent from "main/components/Onboarding/OnboardingSelectCoursesComponent";
import { MemoryRouter } from "react-router";

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
describe("OnboardingSelectCoursesComponent", () => {
  test("Student courses table renders", async () => {
    axiosMock
      .onGet("/api/courses/list")
      .reply(200, coursesFixtures.oneCourseWithEachStatus);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <OnboardingSelectCoursesComponent />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByTestId("OnboardingCoursesTable-cell-row-0-col-id");
  });
});
