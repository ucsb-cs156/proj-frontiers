import OurTable, { ButtonColumn } from "main/components/OurTable";

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
    Header: " GitHub Username",
    accessor: "githubLogin",
  },
  {
    Header: "GitHub ID",
    accessor: "githubId",
  },
];

export default function RosterStudentTable({
  rosterStudents,
  showButtons = false,
  storybook = false,
}) {
  const editCallback = (cell) => {
    const id = cell.row.values.id;
    const msg = `Edit clicked for row with id: ${id}`;
    if (storybook) {
      window.alert(msg);
    } else {
      console.log(msg);
    }
  };

  const deleteCallback = (cell) => {
    const id = cell.row.values.id;
    const msg = `Delete clicked for row with id: ${id}`;
    if (storybook) {
      window.alert(msg);
    } else {
      console.log(msg);
    }
  };

  const columnsToDisplay = showButtons
    ? [
        ...columns,
        ButtonColumn("Edit", "primary", editCallback, "RosterStudentsTable"),
        ButtonColumn("Delete", "danger", deleteCallback, "RosterStudentsTable"),
      ]
    : columns;

  return (
    <OurTable
      data={rosterStudents}
      columns={columnsToDisplay}
      testid={"RosterStudentsTable"}
    />
  );
}
