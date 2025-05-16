import OurTable, { ButtonColumn } from "main/components/OurTable";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";

const columns = [
  {
    Header: "id",
    accessor: "id", // accessor is the "key" in the data
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
  storybook = false,
}) {
  const editCallback = (cell) => {
    const url = `/api/rosterstudents/edit/${cell.row.values.id}`;
    if (storybook) {
      window.alert(`would have navigated to: ${url}`);
      return;
    }
    window.location.href = url;
  };

  // Stryker disable all : hard to test for query caching
  const deleteMutation = useBackendMutation(
    cellToAxiosParamsDelete,
    { onSuccess: onDeleteSuccess },
    ["/api/rosterstudents/all"],
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

function onDeleteSuccess(message) {
  console.log(message);
  toast(message);
}

function cellToAxiosParamsDelete(cell) {
  return {
    url: "/api/rosterstudents",
    method: "DELETE",
    params: {
      id: cell.row.values.id,
    },
  };
}