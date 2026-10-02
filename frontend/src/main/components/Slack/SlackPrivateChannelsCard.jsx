import React from "react";
import { Accordion, Button } from "react-bootstrap";
import { toast } from "react-toastify";
import { useBackendMutation } from "main/utils/useBackend";

export default function SlackPrivateChannelsCard({
  courseId,
  testIdPrefix = "SlackPrivateChannelsCard",
}) {
  const objectToAxiosParams = () => ({
    url: "/api/courses/slack/privateChannels",
    method: "POST",
    params: { courseId },
  });

  const onSuccess = (job) => {
    toast(
      `Job ${job.id} to create private Slack channels started; see the Jobs tab for its log.`,
    );
  };

  const onError = (error) => {
    toast(
      error.response?.data?.message ??
        `Error starting job to create private Slack channels: ${error}`,
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
      data-testid={`${testIdPrefix}-private-channels-card`}
    >
      <Accordion flush>
        <Accordion.Item eventKey="privateChannels">
          <Accordion.Header>
            Slack Private Channels for Each Student plus Staff
          </Accordion.Header>
          <Accordion.Body>
            <p data-testid={`${testIdPrefix}-private-channels-description`}>
              For each student on the roster who has an account in the Slack
              workspace, this creates a private Slack channel named{" "}
              <code>private-first-last</code>, and adds the student, the
              instructor and all of the course staff to it. The first and last
              name are taken from Slack when the student has entered them there,
              and otherwise from the roster; only the first word of the first
              name is used. A student&apos;s channel is recognized by its
              members, not by its name, so running this again does not create
              duplicates: it creates channels for new students, adds new staff
              members to the existing channels, and renames a channel if the
              name of its student has changed. Nobody is ever removed from a
              channel. What was done is logged on the Jobs tab.
            </p>
            <Button
              onClick={() => mutation.mutate()}
              data-testid={`${testIdPrefix}-private-channels-submit`}
            >
              Create Private Channels for Each Student plus Staff
            </Button>
          </Accordion.Body>
        </Accordion.Item>
      </Accordion>
    </div>
  );
}
