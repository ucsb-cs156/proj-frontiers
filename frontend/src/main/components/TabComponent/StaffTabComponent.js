import { toast } from "react-toastify";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import React, { useState } from "react";
import {
  Button,
  Col,
  Form,
  ModalBody,
  ModalHeader,
  Row,
  OverlayTrigger,
  Tooltip,
} from "react-bootstrap";
import CourseStaffForm from "main/components/CourseStaff/CourseStaffForm";
import CourseStaffTable from "main/components/CourseStaff/CourseStaffTable";
import Modal from "react-bootstrap/Modal";

export default function StaffTabComponent({
  courseId,
  testIdPrefix,
  currentUser,
}) {
  const [postModal, showPostModal] = useState(false);
  const [csvModal, showCsvModal] = useState(false);
  const { data: courseStaff } = useBackend(
    [`/api/coursestaff/course?courseId=${courseId}`],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/coursestaff/course?courseId=${courseId}` },
    [],
    true,
  );
  const [searchTerm, setSearchTerm] = useState("");

  const objectToAxiosParamsPost = (staff) => ({
    url: `/api/coursestaff/post`,
    method: "POST",
    params: {
      courseId: courseId,
      firstName: staff.firstName,
      lastName: staff.lastName,
      email: staff.email,
    },
  });

  const onSuccessStaff = (modalFn) => {
    toast("Staff roster successfully updated.");
    // Clear the search filter to show the updated roster
    setSearchTerm("");
    modalFn(false);
  };

  const staffPostMutation = useBackendMutation(
    objectToAxiosParamsPost,
    { onSuccess: () => onSuccessStaff(showPostModal) },
    [`/api/coursestaff/course?courseId=${courseId}`],
  );

  const handlePostSubmit = (staff) => {
    staffPostMutation.mutate(staff);
  };

  // Disabled CSV functions - not implemented yet
  const handleCsvSubmit = () => {
    // No-op for now since backend doesn't support this yet
  };

  const downloadCsv = () => {
    // No-op for now since backend doesn't support this yet
  };

  // Render tooltip for disabled buttons
  const renderComingSoonTooltip = (props) => (
    <Tooltip id="coming-soon-tooltip" {...props}>
      Coming Soon
    </Tooltip>
  );

  console.log("courseStaff=", courseStaff);

  return (
    <div data-testid={`${testIdPrefix}-StaffTabComponent`}>
      <Modal
        show={postModal}
        onHide={() => showPostModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-post-modal`}
      >
        <ModalHeader closeButton>Add Staff Member</ModalHeader>
        <ModalBody>
          <CourseStaffForm
            submitAction={handlePostSubmit}
            cancelDisabled={true}
          />
        </ModalBody>
      </Modal>
      <Row className="mb-1">
        <Form>
          <Form.Group as={Row} controlId="searchFilter">
            <Form.Label column sm={2}>
              Search Staff:
            </Form.Label>
            <Col sm={10}>
              <Form.Control
                type="text"
                placeholder="Search by name, email, or Github Login"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                data-testid={`${testIdPrefix}-search`}
              />
            </Col>
          </Form.Group>
        </Form>
      </Row>
      <Row sm={3} className="p-2">
        <Col>
          <OverlayTrigger placement="top" overlay={renderComingSoonTooltip}>
            <span className="d-inline-block w-100">
              <Button
                data-testid={`${testIdPrefix}-csv-button`}
                className="w-100"
                disabled
                style={{ pointerEvents: "none" }}
              >
                Upload CSV Roster
              </Button>
            </span>
          </OverlayTrigger>
        </Col>
        <Col>
          <Button
            onClick={() => showPostModal(true)}
            data-testid={`${testIdPrefix}-post-button`}
            className="w-100"
          >
            Add Staff Member
          </Button>
        </Col>
        <Col>
          <OverlayTrigger placement="top" overlay={renderComingSoonTooltip}>
            <span className="d-inline-block w-100">
              <Button
                onClick={downloadCsv}
                className="w-100"
                disabled
                style={{ pointerEvents: "none" }}
              >
                Download Staff CSV
              </Button>
            </span>
          </OverlayTrigger>
        </Col>
      </Row>
      <Row>
        <CourseStaffTable
          staff={courseStaff.filter((staffMember) => {
            const searchTermLower = searchTerm.toLowerCase();
            const fullName = `${staffMember.firstName} ${staffMember.lastName}`;
            if (staffMember.email.toLowerCase().includes(searchTermLower)) {
              return true;
            } else if (
              staffMember.githubLogin?.toLowerCase().includes(searchTermLower)
            ) {
              return true;
            } else if (fullName.toLowerCase().includes(searchTermLower)) {
              return true;
            }
            return false;
          })}
          currentUser={currentUser}
          courseId={courseId}
          testIdPrefix={`${testIdPrefix}-CourseStaffTable`}
        />
      </Row>
    </div>
  );
}
