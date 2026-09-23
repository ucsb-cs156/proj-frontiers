import React from "react";
import { Accordion, Button } from "react-bootstrap";
import { toast } from "react-toastify";
import { useBackendMutation } from "main/utils/useBackend";

export default function SlackSectionChannelsCard({
  courseId,
  testIdPrefix = "SlackSectionChannelsCard",
}) {
  const objectToAxiosParams = () => ({
    url: "/api/courses/slack/sectionChannels",
    method: "POST",
    params: { courseId },
  });

  const onSuccess = (job) => {
    toast(
      `Job ${job.id} to set up section Slack channels started; see the Jobs tab for its log.`,
    );
  };

  const onError = (error) => {
    toast(
      error.response?.data?.message ??
        `Error starting job to set up section Slack channels: ${error}`,
    );
  };

  // The job appears on the Jobs tab, so that tab's list of jobs is refreshed
  const mutation = useBackendMutation(
    objectToAxiosParams,
    { onSuccess, onError },
    ["/api/jobs/course"],
  );

  return (
    <div
      className="card mt-4"
      data-testid={`${testIdPrefix}-section-channels-card`}
    >
      <Accordion flush>
        <Accordion.Item eventKey="sectionChannels">
          <Accordion.Header>Slack Section Channels</Accordion.Header>
          <Accordion.Body>
            <p data-testid={`${testIdPrefix}-section-channels-description`}>
              For each section on the Sections tab that has a Slack channel
              name, this creates a public Slack channel with that name (unless
              it already exists), and adds the roster students of that section
              who have an account in the Slack workspace. It then{" "}
              <strong>removes</strong> from each of those channels everyone who
              is not a roster student of that section, other than the course
              staff, the instructor, and bots. What was done is logged on the
              Jobs tab.
            </p>
            <Button
              onClick={() => mutation.mutate()}
              data-testid={`${testIdPrefix}-section-channels-submit`}
            >
              Set Up Section Slack Channels
            </Button>
          </Accordion.Body>
        </Accordion.Item>
      </Accordion>
    </div>
  );
}
