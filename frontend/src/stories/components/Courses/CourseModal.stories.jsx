import React, { useState } from "react";
import coursesFixtures from "fixtures/coursesFixtures";
import CourseModal from "main/components/Courses/CourseModal";
import { Button } from "react-bootstrap";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { schoolList } from "fixtures/schoolFixtures";

export default {
  title: "components/Courses/CourseModal",
  component: CourseModal,
};

const QueryWrapper = ({ children }) => {
  const queryClient = new QueryClient();
  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

const Template = (args) => {
  const [modal, setModalState] = useState(false);
  return (
    <QueryWrapper>
      <Button onClick={() => setModalState(true)}>Open Modal</Button>
      <CourseModal
        showModal={modal}
        toggleShowModal={setModalState}
        {...args}
      />
    </QueryWrapper>
  );
};

export const Create = Template.bind({});

Create.args = {
  buttonText: "Create",
  onSubmitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};

Create.parameters = {
  msw: [
    http.get("/api/systemInfo/schools", () => {
      return HttpResponse.json(schoolList, {
        status: 200,
      });
    }),
  ],
};

export const Update = Template.bind({});

Update.args = {
  initialContents: coursesFixtures.severalCourses[0],
  buttonText: "Update",
  onSubmitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};

Update.parameters = {
  msw: [
    http.get("/api/systemInfo/schools", () => {
      return HttpResponse.json(schoolList, {
        status: 200,
      });
    }),
  ],
};
