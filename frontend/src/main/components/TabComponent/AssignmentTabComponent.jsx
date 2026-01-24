import IndividualAssignmentForm from "main/components/Assignments/IndividualAssignmentForm";
import TeamRepositoryAssignmentForm from "main/components/Assignments/TeamRepositoryAssignmentForm";
import { Card, Row, Col } from "react-bootstrap";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";

export default function AssignmentTabComponent({ courseId }) {
  const onSuccessAssignment = () => {
    toast("Repository creation successfully started.");
  };

  const onSuccessTeamAssignment = () => {
    toast("Team repository creation successfully started.");
  };

  const objectToAxiosParamsIndividualAssignment = (assignment) => ({
    url: `/api/repos/createRepos`,
    method: "POST",
    params: {
      courseId: courseId,
      repoPrefix: assignment.repoPrefix,
      isPrivate: assignment.assignmentPrivacy,
      permissions: assignment.permissions,
      creationOption: assignment.creationOption,
    },
  });

  const indvidiualAssignmentMutation = useBackendMutation(
    objectToAxiosParamsIndividualAssignment,
    { onSuccess: onSuccessAssignment },
  );

  const postIndividualAssignment = (assignment) => {
    indvidiualAssignmentMutation.mutate(assignment);
  };

  const objectToAxiosParamsTeamAssignment = (teamAssignment) => ({
    url: `/api/repos/createTeamRepos`,
    method: "POST",
    params: {
      courseId: courseId,
      repoPrefix: teamAssignment.repoPrefix,
      isPrivate: teamAssignment.assignmentPrivacy,
      permissions: teamAssignment.permissions,
    },
  });

  const teamAssignmentMutation = useBackendMutation(
    objectToAxiosParamsTeamAssignment,
    { onSuccess: onSuccessTeamAssignment },
  );

  const postTeamAssignment = (teamAssignment) => {
    teamAssignmentMutation.mutate(teamAssignment);
  };

  return (
    <Row md={2} className="g-2 mb-2" data-testid={"AssignmentTabComponent"}>
      <Col md={6}>
        <Card>
          <Card.Header>Individual Repository Assignment</Card.Header>
          <Card.Body>
            <IndividualAssignmentForm submitAction={postIndividualAssignment} />
          </Card.Body>
        </Card>
      </Col>
      <Col md={6}>
        <Card className="h-100">
          <Card.Header>Team Repository Assignment</Card.Header>
          <Card.Body>
            <TeamRepositoryAssignmentForm submitAction={postTeamAssignment} />
          </Card.Body>
        </Card>
      </Col>
    </Row>
  );
}
