import React, { useState } from "react";
import SectionTranslator from "main/components/Sections/SectionTranslator";
import { sectionsFixtures } from "fixtures/sectionsFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/Sections/SectionTranslator",
  component: SectionTranslator,
};

const Template = (args) => {
  const [section, setSection] = useState(args.section);
  return (
    <SectionTranslator
      {...args}
      section={section}
      onChange={(value) => {
        setSection(value);
        console.log("Section changed to: ", value);
      }}
    />
  );
};

export const NoTranslationYet = Template.bind({});
NoTranslationYet.args = {
  courseId: 7,
  section: "",
};
NoTranslationYet.parameters = {
  msw: [
    http.get("/api/courses/7/sections", () => {
      return HttpResponse.json(sectionsFixtures.threeSections, {
        status: 200,
      });
    }),
  ],
};

export const WithTranslation = Template.bind({});
WithTranslation.args = {
  courseId: 7,
  section: "0200",
};
WithTranslation.parameters = {
  msw: [
    http.get("/api/courses/7/sections", () => {
      return HttpResponse.json(sectionsFixtures.threeSections, {
        status: 200,
      });
    }),
  ],
};
