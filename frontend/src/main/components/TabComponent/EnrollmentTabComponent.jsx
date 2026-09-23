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
import { BsInfoCircle } from "react-icons/bs";
import RosterStudentCSVUploadForm from "main/components/RosterStudent/RosterStudentCSVUploadForm";
import RosterStudentForm from "main/components/RosterStudent/RosterStudentForm";
import RosterStudentTable from "main/components/RosterStudent/RosterStudentTable";
import Modal from "react-bootstrap/Modal";
import DroppedStudentsTable from "main/components/RosterStudent/DroppedStudentsTable";
import PurgeDroppedStudentsModal from "main/components/RosterStudent/PurgeDroppedStudentsModal";
import ConfirmationModal from "main/components/Common/ConfirmationModal";
import { useSectionLabels } from "main/utils/sectionsUtils";

// Value of the "All sections" / "All teams" option of the filter dropdowns.
// The empty string is taken: it is the value of "(no section)" and "(no team)".
export const ALL = "__all__";

const sectionOf = (student) => student.section || "";
const teamsOf = (student) =>
  Array.isArray(student.teams) ? student.teams : [];

export default function EnrollmentTabComponent({
  courseId,
  testIdPrefix,
  currentUser,
  canEditStudents,
  translateSections = false,
  canvasEnabled = false,
}) {
  const [postModal, setPostModal] = useState(false);
  const [csvModal, setCsvModal] = useState(false);
  const [csvErrorModal, setCsvErrorModal] = useState(false);
  const [csvErrorModalData, setCsvErrorModalData] = useState(null);
  const [purgeModal, setPurgeModal] = useState(false);
  const [canvasSyncModal, setCanvasSyncModal] = useState(false);

  const { data: rosterStudents } = useBackend(
    [`/api/rosterstudents/course/${courseId}`],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/rosterstudents/course/${courseId}` },
    [],
    true,
  );
  const [searchTerm, setSearchTerm] = useState("");
  const [sectionFilter, setSectionFilter] = useState(ALL);
  const [teamFilter, setTeamFilter] = useState(ALL);
  const translateSection = useSectionLabels(courseId, translateSections);

  const objectToAxiosParamsCSV = (formData) => {
    const file = new FormData();
    file.append("file", formData.upload[0]);
    return {
      url: `/api/rosterstudents/upload/csv`,
      data: file,
      params: {
        courseId: courseId,
      },
      method: "POST",
    };
  };

  const objectToAxiosParamsPost = (student) => ({
    url: `/api/rosterstudents/post`,
    method: "POST",
    params: {
      courseId: courseId,
      firstName: student.firstName,
      lastName: student.lastName,
      studentId: student.studentId,
      email: student.email,
      section: student.section,
    },
  });

  const onSuccessRoster = (modalFn) => {
    toast("Roster successfully updated.");
    // Clear the search filter to show the updated roster
    setSearchTerm("");
    modalFn(false);
  };

  const rosterPostMutation = useBackendMutation(
    objectToAxiosParamsPost,
    {
      onSuccess: () => onSuccessRoster(setPostModal),
      onError: (error) => {
        toast.error(
          `Error adding student: ${JSON.stringify(error.response.data, null, 2)}`,
        );
      },
    },
    [`/api/rosterstudents/course/${courseId}`],
  );

  const objectToAxiosParamsCanvasSync = () => ({
    url: `/api/courses/canvas/sync/students`,
    method: "POST",
    params: {
      courseId: courseId,
    },
  });

  const canvasSyncMutation = useBackendMutation(
    objectToAxiosParamsCanvasSync,
    {
      onSuccess: () => {
        toast("Roster successfully updated.");
        setSearchTerm("");
      },
      onError: (error) => {
        toast.error(
          `Error loading students from Canvas: ${JSON.stringify(error.response.data, null, 2)}`,
        );
      },
    },
    [`/api/rosterstudents/course/${courseId}`],
  );

  const handleCanvasSync = () => {
    canvasSyncMutation.mutate();
  };

  const rosterCsvMutation = useBackendMutation(
    objectToAxiosParamsCSV,
    {
      onSuccess: () => onSuccessRoster(setCsvModal),
      onError: (error) => {
        if (error.response.status !== 409) {
          toast.error(
            `Error uploading CSV: ${JSON.stringify(error.response.data, null, 2)}`,
          );
        } else {
          setCsvErrorModal(true);
          setCsvErrorModalData(error.response.data.rejected);
          setCsvModal(false);
        }
      },
    },
    [`/api/rosterstudents/course/${courseId}`],
  );

  const objectToAxiosParamsPurge = (formData) => ({
    url: `/api/rosterstudents/purgeDropped`,
    method: "DELETE",
    params: {
      courseId: courseId,
      removeFromOrg: formData.removeFromOrg,
    },
  });

  const purgeMutation = useBackendMutation(
    objectToAxiosParamsPurge,
    {
      onSuccess: (data) => {
        toast(`Purged ${data.deleted} dropped student(s).`);
        if (data.orgRemovalErrors.length > 0) {
          toast.error(
            `Some students could not be removed from the GitHub organization: ${data.orgRemovalErrors.join("; ")}`,
          );
        }
        setPurgeModal(false);
      },
    },
    [`/api/rosterstudents/course/${courseId}`],
  );

  const handlePurgeSubmit = (formData) => {
    purgeMutation.mutate(formData);
  };

  const droppedStudents = rosterStudents.filter(
    (student) => student.rosterStatus === "DROPPED",
  );
  const activeStudents = rosterStudents.filter(
    (student) => student.rosterStatus !== "DROPPED",
  );
  // The choices offered by the dropdowns: every section / team that some
  // active student has, sorted; "" stands for students with none.
  const sectionOptions = [...new Set(activeStudents.map(sectionOf))].sort();
  const teamOptions = [
    ...new Set(
      activeStudents.flatMap((student) =>
        teamsOf(student).length === 0 ? [""] : teamsOf(student),
      ),
    ),
  ].sort();

  const matchesSearch = (student) => {
    const searchTermLower = searchTerm.toLowerCase();
    const fullName = `${student.firstName} ${student.lastName}`;
    if (student.studentId.toLowerCase().includes(searchTermLower)) {
      return true;
    } else if (student.email.toLowerCase().includes(searchTermLower)) {
      return true;
    } else if (student.githubLogin?.toLowerCase().includes(searchTermLower)) {
      return true;
    } else if (fullName.toLowerCase().includes(searchTermLower)) {
      return true;
    }
    return false;
  };
  const matchesSection = (student) =>
    sectionFilter === ALL || sectionOf(student) === sectionFilter;
  const matchesTeam = (student) =>
    teamFilter === ALL ||
    (teamFilter === ""
      ? teamsOf(student).length === 0
      : teamsOf(student).includes(teamFilter));

  const handleCsvSubmit = (formData) => {
    rosterCsvMutation.mutate(formData);
  };

  const handlePostSubmit = (student) => {
    rosterPostMutation.mutate(student);
  };

  const downloadCsv = () => {
    window.open(`/api/csv/rosterstudents?courseId=${courseId}`, "_blank");
  };

  const openCsvHelp = () => {
    window.open("/help/csv", "_blank");
  };

  return (
    <div data-testid={`${testIdPrefix}-EnrollmentTabComponent`}>
      <Modal
        show={csvErrorModal}
        onHide={() => setCsvErrorModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-csv-error-modal`}
        size="lg"
      >
        <ModalHeader closeButton>Upload CSV Roster</ModalHeader>
        <ModalBody>
          <p>
            The following students couldn&apos;t be uploaded to the roster as
            their emails and student IDs match two separate students:
          </p>
          <RosterStudentTable
            students={csvErrorModalData}
            testIdPrefix={`${testIdPrefix}-RosterStudentTable-csv-error`}
          />
        </ModalBody>
      </Modal>
      <Modal
        show={csvModal}
        onHide={() => setCsvModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-csv-modal`}
      >
        <ModalHeader closeButton>Upload CSV Roster</ModalHeader>
        <ModalBody>
          <RosterStudentCSVUploadForm submitAction={handleCsvSubmit} />
        </ModalBody>
      </Modal>
      <Modal
        show={postModal}
        onHide={() => setPostModal(false)}
        centered={true}
        data-testid={`${testIdPrefix}-post-modal`}
      >
        <ModalHeader closeButton>Add Individual Student</ModalHeader>
        <ModalBody>
          <RosterStudentForm
            submitAction={handlePostSubmit}
            cancelDisabled={true}
            courseId={courseId}
            translateSections={translateSections}
          />
        </ModalBody>
      </Modal>
      <ConfirmationModal
        showModal={canvasSyncModal}
        setShowModal={setCanvasSyncModal}
        onYes={handleCanvasSync}
      >
        <div data-testid={`${testIdPrefix}-canvas-sync-confirmation-message`}>
          <p>
            This adds the students enrolled in the Canvas course to the roster,
            and updates the ones that are already on it, including their
            section.
          </p>
          <p>
            <strong>
              Students who are not in the Canvas course will be marked as
              dropped, and removed from the GitHub organization of this course.
            </strong>{" "}
            This applies to students who were loaded from a CSV file or from
            Canvas; students who were added individually are not affected.
          </p>
          <p className="mb-0">
            Before going ahead, make sure that the Canvas course ID on the
            Settings tab is the right one.
          </p>
        </div>
      </ConfirmationModal>
      <Row sm={canvasEnabled ? 4 : 3} className="p-2">
        <Col>
          <div className="d-flex align-items-center position-relative">
            <Button
              onClick={() => setCsvModal(true)}
              data-testid={`${testIdPrefix}-csv-button`}
              className="w-100 pe-5"
            >
              Upload CSV Roster
            </Button>
            <OverlayTrigger
              placement="right"
              overlay={
                <Tooltip id="csv-help-tooltip">CSV Upload Format Help</Tooltip>
              }
            >
              <BsInfoCircle
                onClick={openCsvHelp}
                style={{
                  position: "absolute",
                  top: "50%",
                  right: "0.75rem",
                  transform: "translateY(-50%)",
                  color: "#fff",
                  cursor: "pointer",
                  fontSize: "0.9rem",
                  userSelect: "none",
                }}
                data-testid={`${testIdPrefix}-csv-info-icon`}
              />
            </OverlayTrigger>
          </div>
        </Col>
        {canvasEnabled && (
          <Col>
            <Button
              onClick={() => setCanvasSyncModal(true)}
              data-testid={`${testIdPrefix}-canvas-sync-button`}
              className="w-100"
            >
              Load Students from Canvas
            </Button>
          </Col>
        )}
        <Col>
          <Button
            onClick={() => setPostModal(true)}
            data-testid={`${testIdPrefix}-post-button`}
            className="w-100"
          >
            Add Individual Student
          </Button>
        </Col>
        <Col>
          <Button onClick={downloadCsv} className="w-100">
            Download Student CSV
          </Button>
        </Col>
      </Row>
      <Form className="mb-1">
        <Row className="align-items-end">
          <Col sm={6}>
            <Form.Group controlId="searchFilter">
              <Form.Label>Search Students:</Form.Label>
              <Form.Control
                type="text"
                placeholder="Search by name, email, student ID, or Github Login"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                data-testid={`${testIdPrefix}-search`}
              />
            </Form.Group>
          </Col>
          <Col sm={3}>
            <Form.Group controlId="sectionFilter">
              <Form.Label>Section:</Form.Label>
              <Form.Select
                value={sectionFilter}
                onChange={(e) => setSectionFilter(e.target.value)}
                data-testid={`${testIdPrefix}-section-filter`}
              >
                <option value={ALL}>All sections</option>
                {sectionOptions.map((section) => (
                  <option key={section} value={section}>
                    {section === ""
                      ? "(no section)"
                      : translateSection(section)}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
          </Col>
          <Col sm={3}>
            <Form.Group controlId="teamFilter">
              <Form.Label>Team:</Form.Label>
              <Form.Select
                value={teamFilter}
                onChange={(e) => setTeamFilter(e.target.value)}
                data-testid={`${testIdPrefix}-team-filter`}
              >
                <option value={ALL}>All teams</option>
                {teamOptions.map((team) => (
                  <option key={team} value={team}>
                    {team === "" ? "(no team)" : team}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
          </Col>
        </Row>
      </Form>
      <Row>
        <RosterStudentTable
          students={activeStudents.filter(
            (student) =>
              matchesSearch(student) &&
              matchesSection(student) &&
              matchesTeam(student),
          )}
          currentUser={currentUser}
          courseId={courseId}
          testIdPrefix={`${testIdPrefix}-RosterStudentTable`}
          canEditStudents={canEditStudents}
          translateSections={translateSections}
        />
      </Row>
      <Row>
        <h2>Dropped Students</h2>
        <DroppedStudentsTable
          students={droppedStudents}
          courseId={courseId}
          translateSections={translateSections}
        />
      </Row>
      <PurgeDroppedStudentsModal
        showModal={purgeModal}
        toggleShowModal={setPurgeModal}
        onSubmitAction={handlePurgeSubmit}
        droppedCount={droppedStudents.length}
      />
      <Row className="p-2">
        <Col>
          <Button
            variant="danger"
            onClick={() => setPurgeModal(true)}
            disabled={droppedStudents.length === 0}
            data-testid={`${testIdPrefix}-purge-dropped-button`}
          >
            Purge All Dropped Students
          </Button>
        </Col>
      </Row>
    </div>
  );
}
