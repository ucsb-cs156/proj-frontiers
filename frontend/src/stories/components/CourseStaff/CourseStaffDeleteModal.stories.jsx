import React, { useState } from "react";
import { Button } from "react-bootstrap";
import CourseStaffDeleteModal from "main/components/CourseStaff/CourseStaffDeleteModal";

export default {
  title: "components/CourseStaff/CourseStaffDeleteModal",
  component: CourseStaffDeleteModal,
};

const Template = (args) => {
  const [modal, setModalState] = useState(false);
  return (
    <div>
      <Button onClick={() => setModalState(true)}>Open Modal</Button>
      <CourseStaffDeleteModal
        showModal={modal}
        toggleShowModal={setModalState}
        {...args}
      />
    </div>
  );
};

export const Default = Template.bind({});

Default.args = {
  onSubmitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
