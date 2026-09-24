import React from "react";
import CourseEmailsCard from "main/components/Emails/CourseEmailsCard";
import { http, HttpResponse } from "msw";

export default {
  title: "components/Emails/CourseEmailsCard",
  component: CourseEmailsCard,
};

const Template = (args) => {
  return <CourseEmailsCard {...args} />;
};

const emails = ["cgaucho@ucsb.edu", "ldelplaya@ucsb.edu", "phtcon@ucsb.edu"];

// Answers in whichever format the dropdown asks for
const emailsHandler = http.get("/api/courses/emails", ({ request }) => {
  const format = new URL(request.url).searchParams.get("format");
  return HttpResponse.text(
    format === "COMMA_SEPARATED" ? emails.join(",") : emails.join("\r\n"),
    { status: 200 },
  );
});

export const Staff = Template.bind({});
Staff.args = {
  courseId: 7,
  type: "STAFF",
};
Staff.parameters = {
  msw: [emailsHandler],
};

export const Empty = Template.bind({});
Empty.args = {
  courseId: 7,
  type: "STAFF",
};
Empty.parameters = {
  msw: [
    http.get("/api/courses/emails", () => {
      return HttpResponse.text("", { status: 200 });
    }),
  ],
};
