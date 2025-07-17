import CoursesTable from "main/components/Courses/CoursesTable";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useBackend } from "main/utils/useBackend";
import { useCurrentUser } from "main/utils/currentUser";

export default function HomePageLoggedIn() {
    const { data: currentUser } = useCurrentUser();

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

  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Your Student Courses</h1>
        <CoursesTable courses={courses} testId={"CoursesTable"} currentUser={currentUser} />
        <h1>Your Staff Courses</h1>
        <CoursesTable courses={staffCourses} testId={"StaffCoursesTable"} currentUser={currentUser} />
      </div>
    </BasicLayout>
  );
}
