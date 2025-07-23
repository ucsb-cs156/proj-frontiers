import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import RosterStudentForm from "main/components/RosterStudent/RosterStudentForm";
import { Navigate, useParams } from "react-router-dom";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";
import { useCurrentUser } from "main/utils/currentUser";

import InstructorCoursesTable from "main/components/Courses/InstructorCoursesTable";

export default function RosterStudentCreatePage({ storybook = false }) {
  const courseId = useParams().id;

  const { data: currentUser } = useCurrentUser();

  const testId = "RosterStudentCreatePage";
  const {
    data: course,
    error: _errorCourses,
    status: _statusCourses,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    ["/api/courses/all"],
    // Stryker disable next-line StringLiteral : GET and empty string are equivalent
    { method: "GET", url: `/api/courses/${courseId}` },
    // Stryker disable next-line all : don't test default value of empty list
    null,
  );

  const objectToAxiosParams = (rosterStudent) => ({
    url: "/api/rosterstudents/post",
    method: "POST",
    params: {
      studentId: rosterStudent.studentId,
      firstName: rosterStudent.firstName,
      lastName: rosterStudent.lastName,
      email: rosterStudent.email,
      courseId: courseId,
    },
  });

  const onSuccess = (rosterStudent) => {
    toast(
      `New Roster Student Created - id: ${rosterStudent.id} studentId: ${rosterStudent.studentId}`,
    );
  };

  const mutation = useBackendMutation(
    objectToAxiosParams,
    { onSuccess },
    // Stryker disable next-line all : hard to set up test for caching
    [`/api/rosterstudents/course/${courseId}`], // mutation makes this key stale so that pages relying on it reload
  );

  const { isSuccess } = mutation;

  const onSubmit = async (data) => {
    mutation.mutate(data);
  };

  if (isSuccess && !storybook) {
    const url = `/instructor/courses/${courseId}`;
    if (!storybook) {
      return <Navigate to={url} />;
    }
  }

  if (!course) {
    return (
      <BasicLayout>
        <div className="pt-2">
          <h1>Create New Roster Student</h1>
          <p>Loading...</p>
        </div>
      </BasicLayout>
    );
  }
  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Create New Roster Student</h1>
        <InstructorCoursesTable
          courses={[course]}
          currentUser={currentUser}
          testId={testId}
        />
        <RosterStudentForm submitAction={onSubmit} />
      </div>
    </BasicLayout>
  );
}
