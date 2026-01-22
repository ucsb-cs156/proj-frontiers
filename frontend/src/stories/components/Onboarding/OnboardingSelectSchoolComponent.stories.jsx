import OnboardingSelectSchoolComponent from "main/components/Onboarding/OnboardingSelectSchoolComponent";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Container } from "react-bootstrap";
import { http, HttpResponse } from "msw";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

export default {
  title: "components/Onboarding/OnboardingSelectSchoolComponent",
  component: OnboardingSelectSchoolComponent,
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
      <OnboardingSelectSchoolComponent />
    </Container>
  </QueryWrapper>
);

export const Default = Template.bind({});
Default.parameters = {
  msw: {
    handlers: [
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(
          systemInfoFixtures.withActiveDirectoryAndGoogle,
        );
      }),
    ],
  },
};

export const NoActiveDirectory = Template.bind({});
NoActiveDirectory.parameters = {
  msw: {
    handlers: [
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither);
      }),
    ],
  },
};

export const NoGoogle = Template.bind({});
NoGoogle.parameters = {
  msw: {
    handlers: [
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.withActiveDirectory);
      }),
    ],
  },
};
