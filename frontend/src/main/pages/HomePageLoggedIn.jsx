import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";
import { useCurrentUser } from "main/utils/currentUser";
import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";
import CourseModal from "main/components/Courses/CourseModal";
import Button from "react-bootstrap/Button";
import React from "react";
import { hasRole } from "main/utils/currentUser";
import { StudentCoursesTable } from "main/components/Courses/StudentCoursesTable";

export default function HomePageLoggedIn() {
  const currentUser = useCurrentUser();
  const {
    data: staffCourses,
    error: _staffError,
    status: _staffStatus,
  } = useBackend(
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
    ["/api/courses/allForInstructors"],
    // Stryker disable next-line StringLiteral : The default value for an empty ("") method is GET. Therefore, there is no way to kill a mutation that transforms "GET" to ""
    { method: "GET", url: "/api/courses/allForInstructors" },
    // Stryker disable next-line all : don't test default value of empty list
    [],
    false,
    {
      enabled: Boolean(hasRole(currentUser, "ROLE_INSTRUCTOR")),
    },
  );

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

  const mutation = useBackendMutation(objectToAxiosParams, { onSuccess }, [
    "/api/courses/allForInstructors",
  ]);

  const onSubmit = async (data) => {
    data.school = data.school.key;
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
        <StudentCoursesTable testid={"CoursesTable"} />
        <h1>Your Staff Courses</h1>
        {staffCourses.length === 0 && <p>No staff courses yet.</p>}
        {staffCourses.length > 0 && (
          <InstructorCoursesTable
            courses={staffCourses}
            currentUser={currentUser}
            testId={"StaffCoursesTable"}
            courseNameLinkPrefix="/staff/courses"
            canEditCourse={true}
            canInstallCourse={(row) => !row.original.orgName}
            mutationQueryKeys={["/api/courses/staffCourses"]}
          />
        )}
      </div>
    </BasicLayout>
  );
}
