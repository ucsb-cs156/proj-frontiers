import React from "react";
import { Accordion, Button } from "react-bootstrap";

import { useBackendMutation } from "main/utils/useBackend";
import { hasRole } from "main/utils/currentUser";
import OurTable, { ButtonColumn } from "main/components/OurTable";
import { toast } from "react-toastify";
import { useState } from "react";

import Modal from "react-bootstrap/Modal";
import { ModalBody, ModalHeader } from "react-bootstrap";
import TeamMemberForm from "main/components/Teams/TeamMemberForm";

export default function TeamsTable({
  teams,
  currentUser,
  courseId,
  testIdPrefix = "TeamsTable",
}) {
  const [postModal, setPostModal] = useState(false);
  const [selectedTeam, setSelectedTeam] = useState(null);
  const [errorPostMemberModal, setErrorPostMemberModal] = useState(false);

  const onSuccessMember = (modalFn) => {
    toast("Member added successfully");
    modalFn(false);
  };

  const onDeleteMember = () => {
    toast("Member removed successfully");
  };

  const onDeleteTeam = () => {
    toast("Team removed successfully");
  };


  const cellToAxiosParamsPost = (data) => ({
    url: `/api/teams/addMember`,
    method: "POST",
    params: {
      courseId: courseId,
      teamId: selectedTeam.id,
      rosterStudentId: data.rosterStudentId,
    },
  });

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
    { onSuccess: onDeleteTeam },
    [`/api/teams/all?courseId=${courseId}`],
  );

  const deleteMemberMutation = useBackendMutation(
    cellToAxiosParamDeleteMember,
    { onSuccess: onDeleteMember },
    [`/api/teams/all?courseId=${courseId}`],
  );

  const memberPostMutation = useBackendMutation(
    cellToAxiosParamsPost,
    {
      onSuccess: () => onSuccessMember(setPostModal),
      onError: (error) => {
        setPostModal(false);
        if (error.response.status === 409) {
          setErrorPostMemberModal({
            message: `This member is already in this team.`,
          });
        } else {
          setErrorPostMemberModal({
            message: `${JSON.stringify(error.response.data.status)} error occurred while adding Member. ${JSON.stringify(error.response.data, null, 2)}`,
          });
        }
      }
    },
    [`/api/teams/all?courseId=${courseId}`],
  );

  const handlePostSubmit = (data) => {
    memberPostMutation.mutate(data);
  };

  const deleteTeamCallback = async (team) => {
    deleteTeamMutation.mutate(team);  
  };

  const deleteMemberCallback = async (cell) => {
    const member = cell.row.original;
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

  return (
    <>
      <Modal
        show={postModal}
        onHide={() => {
          setPostModal(false);
          setSelectedTeam(null);
        }}
        centered={true}
        data-testid={`${testIdPrefix}-post-modal`}
      >
        <ModalHeader closeButton>Add Team Member</ModalHeader>
        <ModalBody>
          <TeamMemberForm submitAction={handlePostSubmit} />
        </ModalBody>
      </Modal>
       <Modal
        show={errorPostMemberModal}
        onHide={() => setErrorPostMemberModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-error-post-member-modal`}
      >
        <ModalHeader closeButton>
          <h4 className="text-danger"> Error Creating Member </h4>
        </ModalHeader>
        <ModalBody>{errorPostMemberModal.message}</ModalBody>
      </Modal>
      <Accordion data-testid={`${testIdPrefix}-accordion`}>
        {teams.map((team, index) => (
          <Accordion.Item eventKey={index.toString()} key={team.id}>
            <Accordion.Header>
              <span className="d-flex align-items-center justify-content-between w-100">
                <h3 data-testid={`${testIdPrefix}-${team.id}-name`}>
                  {team.name}
                </h3>

                {hasRole(currentUser, "ROLE_INSTRUCTOR") && (
                  <span className="ms-auto me-3">
                    <Button
                      onClick={() => {
                        setPostModal(true);
                        setSelectedTeam(team);
                      }}
                      data-testid={`${testIdPrefix}-${team.id}-add-member-button`}
                      className="me-3"
                    >
                      Add Team Member
                    </Button>
                    <Button
                      variant="danger"
                      onClick={(e) => {
                        e.stopPropagation();
                        deleteTeamCallback(team);
                      }}
                      data-testid={`${testIdPrefix}-${team.id}-delete-button`}
                    >
                      Delete
                    </Button>
                  </span>
                )}
              </span>
            </Accordion.Header>
            <Accordion.Body>
              <OurTable
                data={team.teamMembers}
                columns={[
                  ...memberColumns,
                  ...(hasRole(currentUser, "ROLE_INSTRUCTOR")
                    ? [
                        ButtonColumn(
                          "Remove",
                          "danger",
                          deleteMemberCallback,
                          `${testIdPrefix}-${team.id}`,
                        ),
                      ]
                    : []),
                ]}
                testid={`${testIdPrefix}-${team.id}-members-table`}
              />
            </Accordion.Body>
          </Accordion.Item>
        ))}
      </Accordion>
    </>
  );
}
