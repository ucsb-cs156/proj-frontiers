import OurTable, { ButtonColumn } from "main/components/OurTable";
import { useBackendMutation } from "main/utils/useBackend";
import { useNavigate, useParams } from "react-router-dom";

import {
  cellToAxiosParamsDelete,
  onDeleteSuccess,
} from "main/utils/rosterStudentUtils";

const columns = [
  {
    Header: "id",
    accessor: "id", // accessor is the "key" in the data
  },
  {
    Header: "Student ID",
    accessor: "studentId",
  },
  {
    Header: "First Name",
    accessor: "firstName",
  },
  {
    Header: "Last Name",
    accessor: "lastName",
  },
  {
    Header: "Email",
    accessor: "email",
  },
  {
    Header: "Roster Status",
    accessor: "rosterStatus",
  },
  {
    Header: "Github Org Status",
    accessor: "orgStatus",
  },
  {
    Header: "Github ID",
    accessor: "githubId",
  },
  {
    Header: "Github Username",
    accessor: "githubLogin",
  },
];

export default function RosterStudentsTable({
  rosterStudents,
  showButtons = false,
}) {
  const navigate = useNavigate();

  const editCallback = (cell) => {
    navigate(`edit/${cell.row.values.id}`);
  };

  const { courseId } = useParams();
  const key = `/api/rosterstudents/course?courseId=${courseId}`

  // Stryker disable all : hard to test for query caching
  const deleteMutation = useBackendMutation(
    cellToAxiosParamsDelete,
    { onSuccess: onDeleteSuccess },
    [key],
  );
  // Stryker restore all

  // Stryker disable next-line all : TODO try to make a good test for this
  const deleteCallback = async (cell) => {
    deleteMutation.mutate(cell);
  };

  const buttonColumns = [
    ...columns,
    ButtonColumn("Edit", "primary", editCallback, "RosterStudentsTable"),
    ButtonColumn("Delete", "danger", deleteCallback, "RosterStudentsTable"),
  ];

  return (
    <OurTable
      data={rosterStudents}
      columns={showButtons ? buttonColumns : columns}
      testid={"RosterStudentsTable"}
    />
  );
}
