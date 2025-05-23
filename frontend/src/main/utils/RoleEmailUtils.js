import { toast } from "react-toastify";

export function onDeleteSuccess(message) {
  console.log(message);
  toast(message);
}

export function cellToAxiosParamsDelete(cell, role) {
  return {
    url: `/api/admin/${role.toLowerCase()}`,
    method: "DELETE",
    params: {
      email: cell.row.values.email,
    },
  };
}
