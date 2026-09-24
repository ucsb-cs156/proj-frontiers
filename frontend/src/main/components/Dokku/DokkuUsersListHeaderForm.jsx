import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";

function DokkuUsersListHeaderForm({
  initialContents = "",
  submitAction,
  buttonLabel = "Save Header",
}) {
  // `values` (rather than defaultValues) keeps the textarea in sync with the
  // header as it is loaded from the backend after the form first renders.
  // Stryker disable all
  const { register, handleSubmit } = useForm({
    values: { dokkuUsersListHeader: initialContents },
  });
  // Stryker restore all

  const testIdPrefix = "DokkuUsersListHeaderForm";

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="dokkuUsersListHeader">
          Extra lines at the start of dokku_users_list.csv
        </Form.Label>
        <Form.Control
          as="textarea"
          rows={6}
          data-testid={testIdPrefix + "-dokkuUsersListHeader"}
          id="dokkuUsersListHeader"
          placeholder={"username,dokku-00\nusername,dokku-01"}
          {...register("dokkuUsersListHeader")}
        />
      </Form.Group>

      <Button type="submit" data-testid={testIdPrefix + "-submit"}>
        {buttonLabel}
      </Button>
    </Form>
  );
}

export default DokkuUsersListHeaderForm;
