import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { apiCurrentUserFixturesWithGithub } from "fixtures/currentUserFixtures";
import OnboardingSuccessPage from "main/pages/Onboarding/OnboardingSuccessPage";

export default {
  title: "pages/Onboarding/OnboardingSuccessPage",
  component: OnboardingSuccessPage,
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
    <OnboardingSuccessPage />
  </QueryWrapper>
);

export const Default = Template.bind({});
Default.parameters = {
  msw: {
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixturesWithGithub.userOnly);
      }),
    ],
  },
};
