export function cellToAxiosParamsDelete(formData) {
  return {
    url: "/api/rosterstudents/delete",
    method: "DELETE",
    params: {
      id: formData.id,
      removeFromOrg: formData.removeFromOrg,
    },
  };
}
