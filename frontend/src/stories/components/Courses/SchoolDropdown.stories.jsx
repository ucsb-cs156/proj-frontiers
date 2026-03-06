import { SchoolTypeahead } from "main/components/Courses/SchoolTypeahead";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { schoolFixtures, schoolList } from "fixtures/schoolFixtures";
import { http, HttpResponse } from "msw";
import { useForm } from "react-hook-form";

const QueryWrapper = ({ children }) => {
  const queryClient = new QueryClient();
  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

export default {
  title: "components/Courses/SchoolTypeahead",
  component: SchoolTypeahead,
};

const Template = (args) => {
  const { control } = useForm({
    defaultValues: {
      school: args.initialData,
    },
  });
  return (
    <QueryWrapper>
      <SchoolTypeahead control={control} rules={{}} testid="school-typeahead" />
    </QueryWrapper>
  );
};

export const Default = Template.bind({});

Default.parameters = {
  msw: [
    http.get("/api/systemInfo/schools", () => {
      return HttpResponse.json(schoolList, {
        status: 200,
      });
    }),
  ],
};

export const WithInitialData = Template.bind({});
WithInitialData.args = {
  initialData: schoolFixtures.ucsb,
};

WithInitialData.parameters = {
  msw: [
    http.get("/api/systemInfo/schools", () => {
      return HttpResponse.json(schoolList, {
        status: 200,
      });
    }),
  ],
};
