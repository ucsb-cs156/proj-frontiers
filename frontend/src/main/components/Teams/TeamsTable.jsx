import React from "react";
import { Accordion, Button } from "react-bootstrap";

import { useBackendMutation } from "main/utils/useBackend";
import { onDeleteSuccess } from "main/utils/rosterStudentUtils";
import { hasRole } from "main/utils/currentUser";
import OurTable, { ButtonColumn } from "main/components/OurTable";
import { test } from "vitest";

export default function TeamsTable({
  teams,
  currentUser,
  courseId,
  testIdPrefix = "TeamsTable",
}) {
  const cellToAxiosParamDeleteTeam = (team) => ({
    method: "DELETE",
    url: `/api/teams`,
    params: { id: team.id, courseId: courseId },
  });

  const cellToAxiosParamDeleteMember = (member) => ({
    method: "DELETE",
    url: `/api/teams/removeMember`,
    params: { teamMemberId: member.id, courseId: courseId },
  });

  const deleteTeamMutation = useBackendMutation(
    cellToAxiosParamDeleteTeam,
    { onSuccess: onDeleteSuccess },
    [`/api/teams/all?courseId=${courseId}`],
  );

  const deleteMemberMutation = useBackendMutation(
    cellToAxiosParamDeleteMember,
    { onSuccess: onDeleteSuccess },
    [`/api/teams/all?courseId=${courseId}`],
  );

  const deleteTeamCallback = async (team) => {
    deleteTeamMutation.mutate(team);
  };

  const deleteMemberCallback = async (member) => {
    deleteMemberMutation.mutate(member);
  };

  const memberColumns = [
    {
      Header: "First Name",
      accessor: "rosterStudent.firstName",
    },
    {
      Header: "Last Name",
      accessor: "rosterStudent.lastName",
    },
    {
      Header: "Email",
      accessor: "rosterStudent.email",
    },
    {
      Header: "GitHub Login",
      accessor: "rosterStudent.githubLogin",
    },
  ];
  if (hasRole(currentUser, "ROLE_INSTRUCTOR")) {
    memberColumns.push(
      ButtonColumn(
        "Remove",
        "danger",
        deleteMemberCallback,
        testIdPrefix
      ),
    );
  }

  return (
    <>
      <Accordion data-testid={`${testIdPrefix}-accordion`}>
        {teams.map((team, index) => (
          <Accordion.Item eventKey={index.toString()} key={team.id}>
            <Accordion.Header>
              <span className="d-flex align-items-center justify-content-between w-100">
                <h3 data-testid={`${testIdPrefix}-${team.id}-name`}>
                  {team.name}{" "}
                </h3>

                {hasRole(currentUser, "ROLE_INSTRUCTOR") && (
                  <Button
                    variant="danger"
                    onClick={(e) => {
                      e.stopPropagation();
                      deleteTeamCallback(team);
                    }}
                    data-testid={`${testIdPrefix}-${team.id}-delete-button`}
                    className="ms-auto me-3"
                  >
                    Delete
                  </Button>
                )}
              </span>
            </Accordion.Header>
            <Accordion.Body>
              <OurTable
                data={team.teamMembers}
                columns={memberColumns}
                testid={`${testIdPrefix}-${team.id}-members-table`}
              />
            </Accordion.Body>
          </Accordion.Item>
        ))}
      </Accordion>
    </>
  );
}
