import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Container } from "react-bootstrap";
import { http, HttpResponse } from "msw";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import OnboardingSelectCoursesComponent from "main/components/Onboarding/OnboardingSelectCoursesComponent";
import coursesFixtures from "fixtures/coursesFixtures";
import { apiCurrentUserFixturesWithGithub } from "fixtures/currentUserFixtures";

export default {
  title: "components/Onboarding/OnboardingSelectCoursesComponent",
  component: OnboardingSelectCoursesComponent,
};

const QueryWrapper = ({ children }) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false, // Don't retry failed queries in Storybook
        cacheTime: 0, // Don't cache in Storybook
      },
    },
  });

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};
const Template = () => (
  <QueryWrapper>
    <Container>
      <OnboardingSelectCoursesComponent />
    </Container>
  </QueryWrapper>
);

export const Default = Template.bind({});
Default.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixturesWithGithub.userOnly);
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither);
      }),
      http.get("/api/courses/list", () => {
        return HttpResponse.json(
          coursesFixtures.oneRosterStudentWithEachStatus,
        );
      }),
      http.put("/api/rosterstudents/joinCourse", () => {
        return HttpResponse.json("Joining course successful", {
          status: 202,
        });
      }),
    ],
  },
};

export const NoCourses = Template.bind({});
NoCourses.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixturesWithGithub.userOnly);
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither);
      }),
      http.get("/api/courses/list", () => {
        return HttpResponse.json([]);
      }),
    ],
  },
};
