import React, { useState } from "react";
import { Button } from "react-bootstrap";
import SanityCheckModal from "main/components/Common/SanityCheckModal";

export default {
  title: "components/Common/SanityCheckModal",
  component: SanityCheckModal,
};

const Template = (args) => {
  const [modal, setModalState] = useState(false);
  return (
    <div>
      <Button onClick={() => setModalState(true)}>Open Modal</Button>
      <SanityCheckModal
        showModal={modal}
        setShowModal={setModalState}
        {...args}
      >
        <p>Any child will be placed in the body of the modal.</p>
      </SanityCheckModal>
    </div>
  );
};

export const Default = Template.bind({});

Default.args = {
  onYes: () => {
    window.alert("Clicked yes!");
  },
};
