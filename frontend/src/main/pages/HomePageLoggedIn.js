import CoursesTable from "main/components/Courses/CoursesTable";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";
import { useCurrentUser } from "main/utils/currentUser";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import CourseModal from "main/components/Courses/CourseModal";
import Button from "react-bootstrap/Button";
import React from "react";
import { hasRole } from "main/utils/currentUser";

export default function HomePageLoggedIn() {
  const currentUser = useCurrentUser();

  const {
    data: courses,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/courses/list"],
    // Stryker disable next-line StringLiteral : The default value for an empty ("") method is GET. Therefore, there is no way to kill a mutation that transforms "GET" to ""
    { method: "GET", url: "/api/courses/list" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );
  const {
    data: staffCourses,
    error: _staffError,
    status: _staffStatus,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/courses/staffCourses"],
    // Stryker disable next-line StringLiteral : The default value for an empty ("") method is GET.
    { method: "GET", url: "/api/courses/staffCourses" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );
  const {
    data: instructorCourses,
    error: _instructorError,
    status: _instructorStatus,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/courses/allForInstructors"],
    // Stryker disable next-line StringLiteral : The default value for an empty ("") method is GET. Therefore, there is no way to kill a mutation that transforms "GET" to ""
    { method: "GET", url: "/api/courses/allForInstructors" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
  );

  const onJoinSuccess = (message) => {
    toast(message);
  };

  const onJoinFail = (result) => {
    toast(result.response.data ? result.response.data : result.message);
  };

  const cellToAxiosParamsStudent = (cell) => {
    return {
      url: `/api/rosterstudents/joinCourse`,
      method: "PUT",
      params: {
        rosterStudentId: cell.row.original.rosterStudentId,
      },
    };
  };

  const cellToAxiosParamsStaff = (cell) => ({
    url: `/api/coursestaff/joinCourse`,
    method: "PUT",
    params: {
      courseStaffId: cell.row.original.staffId,
    },
  });

  const studentJoinMutation = useBackendMutation(
    cellToAxiosParamsStudent,
    { onSuccess: onJoinSuccess, onError: onJoinFail },
    [`/api/courses/list`],
  );

  const staffJoinMutation = useBackendMutation(
    cellToAxiosParamsStaff,
    { onSuccess: onJoinSuccess, onError: onJoinFail },
    [`/api/courses/staffCourses`],
  );

  const joinStudentCourseCallback = async (cell) => {
    studentJoinMutation.mutate(cell);
  };

  const joinStaffCourseCallback = async (cell) => {
    staffJoinMutation.mutate(cell);
  };

  const isStudentJoining = (cell) => {
    return (
      studentJoinMutation.isPending &&
      studentJoinMutation.variables.row.index === cell.row.index
    );
  };

  const isStaffJoining = (cell) => {
    return (
      staffJoinMutation.isPending &&
      staffJoinMutation.variables.row.index === cell.row.index
    );
  };

  const [viewModal, setViewModal] = React.useState(false);

  const objectToAxiosParams = (course) => ({
    url: "/api/courses/post",
    method: "POST",
    params: {
      courseName: course.courseName,
      term: course.term,
      school: course.school,
    },
  });

  const onSuccess = (course) => {
    toast(`Course ${course.courseName} created`);
    setViewModal(false);
  };

  const mutation = useBackendMutation(
    objectToAxiosParams,
    { onSuccess },
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/courses/allForInstructors"],
  );

  const onSubmit = async (data) => {
    mutation.mutate(data);
  };

  const createCourse = () => setViewModal(true);

  return (
    <BasicLayout>
      <div className="pt-2">
        {hasRole(currentUser, "ROLE_INSTRUCTOR") && (
          <>
            <CourseModal
              showModal={viewModal}
              toggleShowModal={setViewModal}
              onSubmitAction={onSubmit}
            />
            <Button
              onClick={createCourse}
              style={{ float: "right", marginBottom: 10 }}
              variant="primary"
            >
              Create Course
            </Button>
            <h1>Your Instructor Courses</h1>
            {instructorCourses.length === 0 && (
              <p>
                No instructor courses yet. Click the button above to create one.
              </p>
            )}
            {instructorCourses.length > 0 && (
              <>
                <>
                  <InstructorCoursesTable
                    courses={instructorCourses}
                    currentUser={currentUser}
                  />
                </>
              </>
            )}
          </>
        )}
        <h1>Your Student Courses</h1>
        {courses.length > 0 ? (
          <>
            <CoursesTable
              courses={courses}
              testId={"CoursesTable"}
              joinCallback={joinStudentCourseCallback}
              isLoading={isStudentJoining}
            />
          </>
        ) : (
          <p>You are not enrolled in any student courses yet.</p>
        )}
        {staffCourses.length > 0 && (
          <>
            <h1>Your Staff Courses</h1>
            <CoursesTable
              courses={staffCourses}
              testId={"StaffCoursesTable"}
              joinCallback={joinStaffCourseCallback}
              isLoading={isStaffJoining}
            />
          </>
        )}
      </div>
    </BasicLayout>
  );
}
