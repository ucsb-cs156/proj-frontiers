import OurTable from "main/components/OurTable";

const columns = [
  {
    Header: "id",
    accessor: "id", // accessor is the "key" in the data
  },
  {
    header: "First Name",
    accessorKey: "givenName",
  },
  {
    header: "Last Name",
    accessorKey: "familyName",
  },
  {
    header: "Email",
    accessorKey: "email",
  },
  {
    header: "Admin",
    id: "admin",
    accessorKey: "admin",
    cell: ({ cell }) => {
      return String(cell.getValue());
    }, // convert boolean to string for display
  },
  {
    header: "Instructor",
    id: "instructor",
    accessorKey: "instructor",
    cell: ({ cell }) => {
      return String(cell.getValue());
    }, // convert boolean to string for display
  },
];

export default function UsersTable({ users }) {
  return <OurTable data={users} columns={columns} testid={"UsersTable"} />;
}
