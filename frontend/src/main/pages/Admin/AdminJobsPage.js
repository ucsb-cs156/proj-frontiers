import React from "react";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import JobsTable from "main/components/Jobs/JobsTable";
import { useBackend } from "main/utils/useBackend";
import { Button } from "react-bootstrap";
import Accordion from "react-bootstrap/Accordion";
import SingleButtonJobForm from "main/components/Jobs/SingleButtonJobForm";
import { useBackendMutation } from "main/utils/useBackend";

const AdminJobsPage = () => {
  const refreshJobsIntervalMilliseconds = 5000;

  // UpdateAll job
  const objectToAxiosParamsUpdateAllJob = () => ({
    url: "/api/jobs/launch/updateAll",
    method: "POST",
  });

  // Stryker disable all
  const updateAllJobMutation = useBackendMutation(
    objectToAxiosParamsUpdateAllJob,
    {},
    ["/api/jobs/all"],
  );
  // Stryker restore all

  const submitUpdateAllJob = async () => {
    updateAllJobMutation.mutate();
  };

  // AuditAllCourses job
  const objectToAxiosParamsAuditAllCoursesJob = () => ({
    url: "/api/jobs/launch/auditAllCourses",
    method: "POST",
  });

  // Stryker disable all
  const auditAllCoursesJobMutation = useBackendMutation(
    objectToAxiosParamsAuditAllCoursesJob,
    {},
    ["/api/jobs/all"],
  );
  // Stryker restore all

  const submitAuditAllCoursesJob = async () => {
    auditAllCoursesJobMutation.mutate();
  };

  // purge job
  const objectToAxiosParamsPurgeJobLog = () => ({
    url: "/api/jobs/all",
    method: "DELETE",
  });

  // Stryker disable all
  const purgeJobLogMutation = useBackendMutation(
    objectToAxiosParamsPurgeJobLog,
    {},
    ["/api/jobs/all"],
  );
  // Stryker restore all

  const purgeJobLog = async () => {
    purgeJobLogMutation.mutate();
  };

  // Stryker disable all
  const {
    data: jobs,
    error: _error,
    status: _status,
  } = useBackend(
    ["/api/jobs/all"],
    {
      method: "GET",
      url: "/api/jobs/all",
    },
    [],
    { refetchInterval: refreshJobsIntervalMilliseconds },
  );
  // Stryker restore all

  const jobLaunchers = [
    {
      name: "Update All Users",
      form: (
        <SingleButtonJobForm
          callback={submitUpdateAllJob}
          text={"Update All Users"}
        />
      ),
    },
    {
      name: "Audit All Courses",
      form: (
        <SingleButtonJobForm
          callback={submitAuditAllCoursesJob}
          text={"Audit All Courses"}
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
};

export default AdminJobsPage;
