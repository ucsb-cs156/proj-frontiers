import CoursesTable from "main/components/Courses/CoursesTable";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useBackend, useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";

export default function HomePageLoggedIn() {
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
      studentJoinMutation.isLoading &&
      studentJoinMutation.variables.row.index === cell.row.index
    );
  };

  const isStaffJoining = (cell) => {
    return (
      staffJoinMutation.isLoading &&
      staffJoinMutation.variables.row.index === cell.row.index
    );
  };

  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Your Student Courses</h1>
        <CoursesTable
          courses={courses}
          testId={"CoursesTable"}
          joinCallback={joinStudentCourseCallback}
          isLoading={isStudentJoining}
        />
        <h1>Your Staff Courses</h1>
        <CoursesTable
          courses={staffCourses}
          testId={"StaffCoursesTable"}
          joinCallback={joinStaffCourseCallback}
          isLoading={isStaffJoining}
        />
      </div>
    </BasicLayout>
  );
}
