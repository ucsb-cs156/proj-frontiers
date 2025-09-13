import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";
import OurTable, { ButtonColumn } from "main/components/OurTable";

export default function DroppedStudentsTable({ students, courseId }) {
  const columns = [
    {
      header: "id",
      accessorKey: "id",
      id: "id",
    },

    {
      header: "Student Id",
      accessorKey: "studentId",
    },

    {
      header: "First Name",
      accessorKey: "firstName",
    },
    {
      header: "Last Name",
      accessorKey: "lastName",
    },
    {
      header: "Email",
      accessorKey: "email",
    },
  ];

  const cellToAxiosParamsRestore = (cell) => ({
    url: `/api/rosterstudents/restore`,
    method: "PUT",
    params: {
      id: cell.row.original.id,
    },
  });

  const restoreSuccess = () => {
    toast("Student successfully restored to course.");
  };

  const restoreMutation = useBackendMutation(
    cellToAxiosParamsRestore,
    {
      onSuccess: restoreSuccess,
    },
    [`/api/rosterstudents/course/${courseId}`],
  );

  const restoreCallback = (cell) => {
    restoreMutation.mutate(cell);
  };

  columns.push(
    ButtonColumn("Restore", "primary", restoreCallback, "RestoreButton"),
  );

  return (
    <OurTable
      columns={columns}
      data={students}
      testid={"DroppedStudentsTable"}
    />
  );
}
