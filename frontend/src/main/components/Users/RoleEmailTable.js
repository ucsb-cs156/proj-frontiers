import OurTable from "main/components/OurTable";
import { ButtonColumn } from "main/components/OurTable";

export default function RoleEmailTable({ data, deleteCallback }) {
  const columns = [
    {
      Header: "Email",
      accessor: "email",
    },
    ButtonColumn("Delete", "danger", deleteCallback, "RoleEmailTable"),
  ];

  return <OurTable 
      data={Array.isArray(items) ? items : []}
      columns={columns} 
      testid={"RoleEmailTable"} />;
}
