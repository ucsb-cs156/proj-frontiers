import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import RoleEmailForm from "main/components/Users/RoleEmailForm";
import { Navigate } from "react-router-dom";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";

export default function AdminsCreatePage({ storybook = false }) {
  const objectToAxiosParams = (admin) => ({
    url: "/api/admin/admins/post",
    method: "POST",
    params: {
      email: admin.email,
    },
  });

  const onSuccess = (admin) => {
    toast(`New admin added - email: ${admin.email}`);
  };

  const mutation = useBackendMutation(
    objectToAxiosParams,
    { onSuccess },
    // Stryker disable next-line all : hard to set up test for caching
    ["/api/admin/admins/all"], // mutation makes this key stale so that pages relying on it reload
  );

  const { isSuccess } = mutation;

  const onSubmit = async (data) => {
    mutation.mutate(data);
  };

  if (isSuccess && !storybook) {
    return <Navigate to="/admin/admins" />;
  }

  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Add New Admin</h1>
        <RoleEmailForm submitAction={onSubmit} />
      </div>
    </BasicLayout>
  );
}
