import React from "react";
import NewApiKeyModal from "main/components/ApiKeys/NewApiKeyModal";
import { apiKeysFixtures } from "fixtures/apiKeysFixtures";

export default {
  title: "components/ApiKeys/NewApiKeyModal",
  component: NewApiKeyModal,
};

const Template = (args) => <NewApiKeyModal {...args} />;

// A fixed host, so that the example request does not change with the host that
// each Chromatic build is served from.
const baseUrl = "https://frontiers.example-host.cs.ucsb.edu";

export const WithLabel = Template.bind({});
WithLabel.args = {
  issuedKey: apiKeysFixtures.issuedKey,
  courseId: 7,
  baseUrl,
  onClose: () => window.alert("Close was clicked"),
};

export const WithoutLabel = Template.bind({});
WithoutLabel.args = {
  issuedKey: apiKeysFixtures.issuedKeyWithoutLabel,
  courseId: 7,
  baseUrl,
  onClose: () => window.alert("Close was clicked"),
};
