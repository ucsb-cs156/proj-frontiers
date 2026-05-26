import { useBackend, useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";
import OurTable from "main/components/OurTable";
import React from "react";
import { Link } from "react-router";


export function StudentCoursesTable({ testid }) {
  const { data: courses } = useBackend(
    ["/api/courses/list"],
    // Stryker disable next-line StringLiteral : The default value for an empty ("") method is GET. Therefore, there is no way to kill a mutation that transforms "GET" to ""
    { method: "GET", url: "/api/courses/list" },
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

  const studentJoinMutation = useBackendMutation(
    cellToAxiosParamsStudent,
    { onSuccess: onJoinSuccess, onError: onJoinFail },
    [`/api/courses/list`],
  );

  const joinStudentCourseCallback = async (cell) => {
    studentJoinMutation.mutate(cell);
  };

  const isStudentJoining = (cell) => {
    return (
      studentJoinMutation.isPending &&
      studentJoinMutation.variables.row.index === cell.row.index
    );
  };

  const columns = [
    {
      header: "id",
      accessorKey: "id", // accessor is the "key" in the data
    },
    {
      header: "Course Name",
      id: "courseName",
      cell: ({ row }) => (
        <Link
          to={`/student/courses/${row.original.id}`}
          data-testid={`${testid}-cell-row-${row.index}-col-courseName-link`}
        >
          {row.original.courseName}
        </Link>
      ),
    },
    {
      header: "Term",
      accessorKey: "term",
    },
    {
      header: "School",
      id: "school",
      accessorKey: "school.displayName",
    },
    {
      header: "Status",
      accessorKey: "studentStatus",
    }
  ];

  return (
    <>
      {courses.length > 0 ? (
        <OurTable data={courses} columns={columns} testid={testid} />
      ) : (
        <p>You are not enrolled in any student courses yet.</p>
      )}
    </>
  );
}
