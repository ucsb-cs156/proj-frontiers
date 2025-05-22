import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import RoleEmailForm from "main/components/Users/RoleEmailForm";
import { Navigate } from "react-router-dom";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";

export default function InstructorsCreatePage({ storybook = false }) {
  const objectToAxiosParams = (instructor) => ({
    url: "/api/admin/instructors/post",
    method: "POST",
    params: {
      email: instructor.email,
    },
  });

  const onSuccess = (instructor) => {
    toast(`New instructor added - email: ${instructor.email}`);
  };

  const mutation = useBackendMutation(
    objectToAxiosParams,
    { onSuccess },
    // Stryker disable next-line all : hard to set up test for caching
    ["/api/admin/instructors/all"], // mutation makes this key stale so that pages relying on it reload
  );

  const { isSuccess } = mutation;

  const onSubmit = async (data) => {
    mutation.mutate(data);
  };

  if (isSuccess && !storybook) {
    return <Navigate to="/admin/instructors" />;
  }

  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Add New Instructor</h1>
        <RoleEmailForm submitAction={onSubmit} />
      </div>
    </BasicLayout>
  );
}
