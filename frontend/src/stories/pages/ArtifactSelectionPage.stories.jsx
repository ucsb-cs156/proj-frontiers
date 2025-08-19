import React from "react";
import ArtifactSelectionPage from "main/pages/ArtifactSelectionPage";
import collectionNames from "fixtures/collectionNames";
import { http, HttpResponse } from "msw";

export default {
  title: "pages/ArtifactSelectionPage",
  component: ArtifactSelectionPage,
};

const Template = () => <ArtifactSelectionPage />;

export const Default = Template.bind({});
Default.parameters = {
  msw: {
    handlers: [
      http.get("/api/collections/list", () => {
        return HttpResponse.json(collectionNames.collectionNamesForOneCourse);
      }),
    ],
  },
};
