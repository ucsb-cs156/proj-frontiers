import React, { useState } from "react";
import { Button } from "react-bootstrap";
import ConfirmationModal from "main/components/Common/ConfirmationModal";

export default {
  title: "components/Common/ConfirmationModal",
  component: ConfirmationModal,
};

const Template = (args) => {
  const [modal, setModalState] = useState(false);
  return (
    <div>
      <Button onClick={() => setModalState(true)}>Open Modal</Button>
      <ConfirmationModal
        showModal={modal}
        setShowModal={setModalState}
        {...args}
      >
        <p>Any child will be placed in the body of the modal.</p>
      </ConfirmationModal>
    </div>
  );
};

export const Default = Template.bind({});

Default.args = {
  onYes: () => {
    window.alert("Clicked yes!");
  },
};
