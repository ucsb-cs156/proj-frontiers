import OurTable from "main/components/OurTable";
import { hasRole } from "main/utils/currentUser";
import { Tooltip, OverlayTrigger, Button } from "react-bootstrap";
import { Link } from "react-router";
import { useState } from "react";
import GithubSettingIcon from "main/components/Common/GithubSettingIcon";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";
import UpdateInstructorForm from "main/components/Courses/UpdateInstructorForm";
import CourseModal from "main/components/Courses/CourseModal";
import Modal from "react-bootstrap/Modal";
import { useLocation } from "react-router";

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
  enableInstructorUpdate = false,
}) {
  const location = useLocation();

  const [showModal, setShowModal] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState(null);
  const [showCourseEditModal, setShowCourseEditModal] = useState(false);
  const [selectedCourseForEdit, setSelectedCourseForEdit] = useState(null);
  const cellToAxiosParamsEdit = (formData) => {
    return {
      url: `/api/courses/updateInstructor`,
      method: "PUT",
      params: {
        courseId: formData.courseId,
        instructorEmail: formData.instructorEmail,
      },
    };
  };

  const cellToAxiosParamsCourseEdit = (formData) => {
    return {
      url: `/api/courses`,
      method: "PUT",
      params: {
        courseId: formData.courseId,
        courseName: formData.courseName,
        term: formData.term,
        school: formData.school,
      },
    };
  };

  const onInstructorUpdateSuccess = () => {
    handleCloseModal();
  };

  const onInstructorUpdateError = (error) => {
    if (error.response.data.message)
      toast(
        `Was not able to update instructor:\n${error.response.data.message}`,
      );
    else toast(`Was not able to update instructor:\n${error.message}`);
  };

  const onCourseUpdateSuccess = () => {
    handleCloseCourseEditModal();
    toast("Course updated successfully");
  };

  const onCourseUpdateError = (error) => {
    if (error.response.data.message)
      toast(`Was not able to update course:\n${error.response.data.message}`);
    else toast(`Was not able to update course:\n${error.message}`);
  };

  const editMutation = useBackendMutation(
    cellToAxiosParamsEdit,
    {
      onSuccess: onInstructorUpdateSuccess,
      onError: onInstructorUpdateError,
    },
    ["/api/courses/allForAdmins", "/api/courses/allForInstructors"],
  );

  const courseEditMutation = useBackendMutation(
    cellToAxiosParamsCourseEdit,
    {
      onSuccess: onCourseUpdateSuccess,
      onError: onCourseUpdateError,
    },
    ["/api/courses/allForAdmins", "/api/courses/allForInstructors"],
  );

  const handleShowModal = (course) => {
    setSelectedCourse(course);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedCourse(null);
  };

  const handleShowCourseEditModal = (course) => {
    setSelectedCourseForEdit(course);
    setShowCourseEditModal(true);
  };

  const handleCloseCourseEditModal = () => {
    setShowCourseEditModal(false);
    setSelectedCourseForEdit(null);
  };

  const handleUpdateInstructor = async (formData) => {
    formData.courseId = selectedCourse.id;
    editMutation.mutate(formData);
  };

  const handleUpdateCourse = async (formData) => {
    formData.courseId = selectedCourseForEdit.id;
    courseEditMutation.mutate(formData);
  };

  const installCallback = (cell) => {
    sessionStorage.setItem("redirect", location.pathname);

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

  const canEdit = (row) => {
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
      set_message = `Click to install the GitHub app for ${cell.row.original.courseName}`;
    } else {
      set_message = `View organization associated with ${cell.row.original.courseName}.`;
    }
    return (
      <Tooltip id={`tooltip-orgname-${cell.row.index}`}>{set_message}</Tooltip>
    );
  };

  const columnsWithInstall = [
    ...columns,
    {
      header: "Edit",
      id: "edit",
      cell: ({ cell }) => {
        const canEditCourse = canEdit(cell.row);
        if (canEditCourse) {
          return (
            <Button
              variant="outline-primary"
              size="sm"
              onClick={() => handleShowCourseEditModal(cell.row.original)}
              data-testid={`${testId}-cell-row-${cell.row.index}-col-edit-button`}
            >
              Edit
            </Button>
          );
        } else {
          return (
            <span
              data-testid={`${testId}-cell-row-${cell.row.index}-col-edit-no-permission`}
            ></span>
          );
        }
      },
    },
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
                  <Tooltip id={`tooltip-geargithubicon-${cell.row.index}`}>
                    Manage settings for association between your GitHub
                    organization and this web application.
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
                    <GithubSettingIcon
                      size={24}
                      data-testid={`CoursesTable-cell-row-${cell.row.index}-col-${cell.column.id}-gear-github-icon`}
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
        if (isAdmin && enableInstructorUpdate) {
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
      <Modal
        data-testid={`${testId}-modal`}
        show={showModal}
        onHide={handleCloseModal}
        centered={true}
      >
        <Modal.Header closeButton>
          <Modal.Title>Update Instructor</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <UpdateInstructorForm
            handleUpdateInstructor={handleUpdateInstructor}
            initialContents={selectedCourse}
          />
        </Modal.Body>
      </Modal>
      <CourseModal
        showModal={showCourseEditModal}
        toggleShowModal={setShowCourseEditModal}
        onSubmitAction={handleUpdateCourse}
        initialContents={selectedCourseForEdit}
        buttonText="Update"
        modalTitle="Edit Course"
      />
      <OurTable data={courses} columns={columnsWithInstall} testid={testId} />
    </>
  );
}
