import React from "react";
import { Accordion, Button } from "react-bootstrap";
import { toast } from "react-toastify";
import { useBackendMutation } from "main/utils/useBackend";

export default function SlackTeamChannelsCard({
  courseId,
  testIdPrefix = "SlackTeamChannelsCard",
}) {
  const objectToAxiosParams = () => ({
    url: "/api/courses/slack/teamChannels",
    method: "POST",
    params: { courseId },
  });

  const onSuccess = (job) => {
    toast(
      `Job ${job.id} to set up team Slack channels started; see the Jobs tab for its log.`,
    );
  };

  const onError = (error) => {
    toast(
      error.response?.data?.message ??
        `Error starting job to set up team Slack channels: ${error}`,
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
      data-testid={`${testIdPrefix}-team-channels-card`}
    >
      <Accordion flush>
        <Accordion.Item eventKey="teamChannels">
          <Accordion.Header>Slack Team Channels</Accordion.Header>
          <Accordion.Body>
            <p data-testid={`${testIdPrefix}-team-channels-description`}>
              For each team on the Teams tab, this creates a public Slack
              channel named <code>team-</code> followed by the team name
              (lowercased, with spaces and other special characters replaced by
              hyphens), unless it already exists. It adds the members of the
              team who have an account in the Slack workspace, and the
              instructor, to the channel. It then <strong>removes</strong> from
              each channel everyone who is not on that team, other than the
              instructor, the course staff, and bots. If two teams would get the
              same channel name, nothing is done and the job reports which
              teams. What was done is logged on the Jobs tab.
            </p>
            <Button
              onClick={() => mutation.mutate()}
              data-testid={`${testIdPrefix}-team-channels-submit`}
            >
              Set Up Team Slack Channels
            </Button>
          </Accordion.Body>
        </Accordion.Item>
      </Accordion>
    </div>
  );
}
