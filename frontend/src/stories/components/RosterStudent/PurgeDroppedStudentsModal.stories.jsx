import React, { useState } from "react";
import { Button } from "react-bootstrap";
import PurgeDroppedStudentsModal from "main/components/RosterStudent/PurgeDroppedStudentsModal";

export default {
  title: "components/RosterStudent/PurgeDroppedStudentsModal",
  component: PurgeDroppedStudentsModal,
};

const Template = (args) => {
  const [modal, setModalState] = useState(false);
  return (
    <div>
      <Button variant="danger" onClick={() => setModalState(true)}>
        Open Modal
      </Button>
      <PurgeDroppedStudentsModal
        showModal={modal}
        toggleShowModal={setModalState}
        {...args}
      />
    </div>
  );
};

export const Default = Template.bind({});

Default.args = {
  droppedCount: 3,
  onSubmitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
