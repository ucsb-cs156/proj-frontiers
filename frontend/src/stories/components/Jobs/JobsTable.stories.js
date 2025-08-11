import React from "react";
import JobsTable from "main/components/Jobs/JobsTable";
import { jobsFixtures } from "fixtures/jobsFixtures";

export default {
  title: "components/Jobs/JobsTable",
  component: JobsTable,
};

const Template = (args) => <JobsTable {...args} />;

export const Empty = Template.bind({});
Empty.args = {
  jobs: [],
};

export const ThreeJobs = Template.bind({});
ThreeJobs.args = {
  jobs: jobsFixtures.threeJobs,
};

export const OneJob = Template.bind({});
OneJob.args = {
  jobs: jobsFixtures.oneJob,
};

export const LongLog = Template.bind({});
LongLog.args = {
  jobs: jobsFixtures.longLogJob,
};