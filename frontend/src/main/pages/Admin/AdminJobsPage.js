import React from "react";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import JobsTable from "main/components/Jobs/JobsTable";
import { useBackend } from "main/utils/useBackend";
import { Button } from "react-bootstrap";
import Accordion from "react-bootstrap/Accordion";
import SingleButtonJobForm from "main/components/Jobs/SingleButtonJobForm";
import { useBackendMutation } from "main/utils/useBackend";

export default function AdminJobsPage() {
  const objectToAxiosParamsUpdateAllJob = () => ({
    url: "/api/jobs/launch/updateAll",
    method: "POST",
  });

  const updateAllJobMutation = useBackendMutation(
    objectToAxiosParamsUpdateAllJob,
    {},
    ["/api/jobs/all"],
  );

  const submitUpdateAllJob = async () => {
    updateAllJobMutation.mutate();
  };

  // AuditAllCourses job
  const objectToAxiosParamsAuditAllCoursesJob = () => ({
    url: "/api/jobs/launch/auditAllCourses",
    method: "POST",
  });

  const auditAllCoursesJobMutation = useBackendMutation(
    objectToAxiosParamsAuditAllCoursesJob,
    {},
    ["/api/jobs/all"],
  );

  const submitAuditAllCoursesJob = async () => {
    auditAllCoursesJobMutation.mutate();
  };

  // purge job
  const objectToAxiosParamsPurgeJobLog = () => ({
    url: "/api/jobs/all",
    method: "DELETE",
  });

  const purgeJobLogMutation = useBackendMutation(
    objectToAxiosParamsPurgeJobLog,
    {},
    ["/api/jobs/all"],
  );

  const purgeJobLog = async () => {
    purgeJobLogMutation.mutate();
  };

  const { data: jobs } = useBackend(
    ["/api/jobs/all"],
    {
      //Stryker disable next-line StringLiteral: axios default is GET
      method: "GET",
      url: "/api/jobs/all",
    },
    [],
  );

  const jobLaunchers = [
    {
      name: "Update All Users",
      form: (
        <SingleButtonJobForm
          callback={submitUpdateAllJob}
          text={"Start"}
          testid={"updateAllJob"}
        />
      ),
    },
    {
      name: "Audit All Courses",
      form: (
        <SingleButtonJobForm
          callback={submitAuditAllCoursesJob}
          text={"Start"}
          testid={"auditAllCoursesJob"}
        />
      ),
    },
  ];

  return (
    <BasicLayout>
      <h2 className="p-3">Launch Jobs</h2>
      <Accordion>
        {jobLaunchers.map((jobLauncher, index) => (
          <Accordion.Item eventKey={index} key={index}>
            <Accordion.Header>{jobLauncher.name}</Accordion.Header>
            <Accordion.Body>{jobLauncher.form}</Accordion.Body>
          </Accordion.Item>
        ))}
      </Accordion>
      <h2 className="p-3">Job Status</h2>
      <JobsTable jobs={jobs} />
      <Button variant="danger" onClick={purgeJobLog} data-testid="purgeJobLog">
        Purge Job Log
      </Button>
    </BasicLayout>
  );
}
