import IndividualAssignmentForm from "main/components/Assignments/IndividualAssignmentForm";
import { Card, Row } from "react-bootstrap";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";

export default function AssignmentTabComponent({ courseId }) {
  const onSuccessAssignment = () => {
    toast("Repository creation successfully started.");
  };

  const objectToAxiosParamsIndividualAssignment = (assignment) => ({
    url: `/api/repos/createRepos`,
    method: "POST",
    params: {
      courseId: courseId,
      repoPrefix: assignment.repoPrefix,
      isPrivate: assignment.assignmentPrivacy,
      permissions: assignment.permissions,
    },
  });

  const indvidiualAssignmentMutation = useBackendMutation(
    objectToAxiosParamsIndividualAssignment,
    { onSuccess: onSuccessAssignment },
  );

  const postIndividualAssignment = (assignment) => {
    indvidiualAssignmentMutation.mutate(assignment);
  };
  return (
    <Row md={2} data-testid={"AssignmentTabComponent"}>
      <Card>
        <Card.Header>Individual Assignment</Card.Header>
        <Card.Body>
          <IndividualAssignmentForm submitAction={postIndividualAssignment} />
        </Card.Body>
      </Card>
    </Row>
  );
}
