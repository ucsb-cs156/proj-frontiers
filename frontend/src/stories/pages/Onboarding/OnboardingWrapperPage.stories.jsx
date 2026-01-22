import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import OnboardingWrapperPage from "main/pages/Onboarding/OnboardingWrapperPage";
import { http, HttpResponse } from "msw";
import {
  apiCurrentUserFixtures,
  apiCurrentUserFixturesWithGithub,
} from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import coursesFixtures from "fixtures/coursesFixtures";

export default {
  title: "pages/Onboarding/OnboardingWrapperPage",
  component: OnboardingWrapperPage,
};

// Create a wrapper that provides React Query context
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
    <OnboardingWrapperPage />
  </QueryWrapper>
);

export const NotLoggedIn = Template.bind({});
NotLoggedIn.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json([], {
          status: 403,
        });
      }),
    ],
  },
};

export const LoggedInNoGithub = Template.bind({});
LoggedInNoGithub.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixtures.userOnly);
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither);
      }),
    ],
  },
};

export const LoggedInWithGithub = Template.bind({});
LoggedInWithGithub.parameters = {
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
