import React, { useState } from "react";
import coursesFixtures from "fixtures/coursesFixtures";
import CourseModal from "main/components/Courses/CourseModal";
import { Button } from "react-bootstrap";

export default {
  title: "components/Courses/CourseModal",
  component: CourseModal,
};

const Template = (args) => {
  const [modal, setModalState] = useState(false);
  return (
    <div>
      <Button onClick={() => setModalState(true)}>Open Modal</Button>
      <CourseModal
        showModal={modal}
        toggleShowModal={setModalState}
        {...args}
      />
    </div>
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

export const Update = Template.bind({});

Update.args = {
  initialContents: coursesFixtures.severalCourses[0],
  buttonText: "Update",
  onSubmitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
