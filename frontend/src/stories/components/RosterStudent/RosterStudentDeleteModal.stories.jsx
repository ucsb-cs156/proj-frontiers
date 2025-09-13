import React, { useState } from "react";
import { Button } from "react-bootstrap";
import RosterStudentDeleteModal from "main/components/RosterStudent/RosterStudentDeleteModal";

export default {
  title: "components/RosterStudent/RosterStudentDeleteModal",
  component: RosterStudentDeleteModal,
};

const Template = (args) => {
  const [modal, setModalState] = useState(false);
  return (
    <div>
      <Button onClick={() => setModalState(true)}>Open Modal</Button>
      <RosterStudentDeleteModal
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
