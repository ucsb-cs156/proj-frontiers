import OurTable from "main/components/OurTable";
import { hasRole } from "main/utils/currentUser";
import { Tooltip, OverlayTrigger, Button, Modal, Form } from "react-bootstrap";
import { FaGithub } from "react-icons/fa";
import { Link } from "react-router";
import { useState } from "react";

const columns = [
  {
    header: "id",
    accessorKey: "id", // accessor is the "key" in the data
  },
  {
    header: "Course Name",
    id: "courseName",
    cell: ({ cell }) => {
      return (
        <OverlayTrigger
          placement="right"
          overlay={
            <Tooltip id={`tooltip-coursename-${cell.row.index}`}>
              View course details
            </Tooltip>
          }
        >
          <Link
            to={`/instructor/courses/${cell.row.original.id}`}
            data-testid={`CoursesTable-cell-row-${cell.row.index}-col-${cell.column.id}-link`}
          >
            {cell.row.original.courseName}
          </Link>
        </OverlayTrigger>
      );
    },
  },
  {
    header: "Term",
    accessorKey: "term",
  },
  {
    header: "School",
    accessorKey: "school",
  },
];

export default function InstructorCoursesTable({
  courses,
  storybook = false,
  currentUser,
  testId = "InstructorCoursesTable",
}) {
  const [showModal, setShowModal] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState(null);
  const [newInstructorEmail, setNewInstructorEmail] = useState(null);
  const [isUpdating, setIsUpdating] = useState(false);

  const handleShowModal = (course) => {
    setSelectedCourse(course);
    setNewInstructorEmail(course.instructorEmail);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedCourse(null);
  };

  const handleUpdateInstructor = async () => {
    setIsUpdating(true);

    if (storybook) {
      window.alert(
        `Would update course ${selectedCourse.id} instructor to: ${newInstructorEmail}`,
      );
      handleCloseModal();
      return;
    }

    try {
      const response = await fetch(
        `/api/courses/updateInstructor?courseId=${selectedCourse.id}&instructorEmail=${encodeURIComponent(newInstructorEmail)}`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
          },
          credentials: "include",
        },
      );

      if (response.ok) {
        window.location.reload(); // Reload to show updated data
      } else {
        const errorData = await response.text();
        window.alert(`Error updating instructor: ${errorData}`);
      }
    } catch (error) {
      window.alert(`Error updating instructor: ${error.message}`);
    } finally {
      handleCloseModal();
    }
  };
  const installCallback = (cell) => {
    const url = `/api/courses/redirect?courseId=${cell.row.original.id}`;
    if (storybook) {
      window.alert(`would have navigated to: ${url}`);
      return;
    }
    window.location.href = url;
  };

  const canInstall = (row) => {
    if (row.original.orgName) {
      return false;
    }
    if (hasRole(currentUser, "ROLE_ADMIN")) {
      return true;
    }
    if (
      hasRole(currentUser, "ROLE_INSTRUCTOR") &&
      row.original.instructorEmail === currentUser.root.user.email
    ) {
      return true;
    }

    return false;
  };

  const renderTooltip = (cell) => {
    let set_message;
    const result = canInstall(cell.row);
    if (result) {
      set_message = `Click to install the GitHub app for the course: ${cell.row.original.courseName}`;
    } else {
      set_message = `View GitHub organization: ${cell.row.original.orgName}`;
    }
    return (
      <Tooltip id={`tooltip-orgname-${cell.row.index}`}>{set_message}</Tooltip>
    );
  };

  const columnsWithInstall = [
    ...columns,
    {
      header: "GitHub Org",
      id: "orgName",
      cell: ({ cell }) => {
        const result = canInstall(cell.row);
        if (result) {
          return (
            <OverlayTrigger placement="right" overlay={renderTooltip(cell)}>
              <span>
                <Button
                  variant={"primary"}
                  onClick={() => installCallback(cell)}
                  data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-button`}
                >
                  Install GitHub App
                </Button>
              </span>
            </OverlayTrigger>
          );
        } else if (!cell.row.original.orgName) {
          return (
            <span
              data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-no-org`}
            ></span>
          );
        } else {
          return (
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                width: "100%",
              }}
              data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-div`}
            >
              <OverlayTrigger placement="right" overlay={renderTooltip(cell)}>
                <span>
                  <a
                    href={`https://github.com/${cell.row.original.orgName}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    data-testid={`${testId}-cell-row-${cell.row.index}-col-${cell.column.id}-github-link`}
                  >
                    {cell.row.original.orgName}
                  </a>
                </span>
              </OverlayTrigger>
              <OverlayTrigger
                placement="right"
                overlay={
                  <Tooltip id={`tooltip-githubicon-${cell.row.index}`}>
                    Manage installation settings for the frontiers app,
                    including the option to uninstall it from this GitHub
                    organization.
                  </Tooltip>
                }
              >
                <span>
                  <a
                    href={`https://github.com/organizations/${cell.row.original.orgName}/settings/installations/${cell.row.original.installationId}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    data-testid={`CoursesTable-cell-row-${cell.row.index}-col-${cell.column.id}-github-settings-link`}
                  >
                    <FaGithub
                      size={"1.5em"}
                      data-testid={`CoursesTable-cell-row-${cell.row.index}-col-${cell.column.id}-github-icon`}
                    />
                  </a>
                </span>
              </OverlayTrigger>
            </div>
          );
        }
      },
    },
    {
      header: "Instructor",
      accessorKey: "instructorEmail",
      cell: ({ cell }) => {
        const isAdmin = hasRole(currentUser, "ROLE_ADMIN");
        if (isAdmin) {
          return (
            <Button
              variant="link"
              onClick={() => handleShowModal(cell.row.original)}
              data-testid={`${testId}-cell-row-${cell.row.index}-col-instructorEmail-button`}
              style={{ padding: 0, textDecoration: "underline" }}
            >
              {cell.row.original.instructorEmail}
            </Button>
          );
        } else {
          return cell.row.original.instructorEmail;
        }
      },
    },
  ];

  return (
    <>
      <OurTable data={courses} columns={columnsWithInstall} testid={testId} />

      <Modal show={showModal} onHide={handleCloseModal}>
        <Modal.Header closeButton>
          <Modal.Title>Update Instructor</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label>Course: {selectedCourse?.courseName}</Form.Label>
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>New Instructor Email</Form.Label>
              <Form.Control
                type="email"
                value={newInstructorEmail}
                onChange={(e) => setNewInstructorEmail(e.target.value)}
                placeholder="Enter instructor email"
                data-testid="update-instructor-email-input"
              />
              <Form.Text className="text-muted">
                Email must belong to an existing instructor or admin.
              </Form.Text>
            </Form.Group>
          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="secondary"
            onClick={handleCloseModal}
            disabled={isUpdating}
          >
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleUpdateInstructor}
            disabled={isUpdating || !newInstructorEmail}
            data-testid="update-instructor-submit-button"
          >
            {isUpdating ? "Updating..." : "Update Instructor"}
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
