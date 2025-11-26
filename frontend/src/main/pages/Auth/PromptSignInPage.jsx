import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { Alert, Row } from "react-bootstrap";
import { useLocation } from "react-router";
import SignInProviders from "main/components/Auth/SignInProviders";

export default function PromptSignInPage() {
  const location = useLocation();

  const setRedirect = () => {
    sessionStorage.setItem("redirect", location.pathname);
  };

  return (
    <BasicLayout>
      <Row className="p-3">
        <Alert variant={"danger"}>
          Please sign in before accessing this page.
        </Alert>
      </Row>
      <SignInProviders onClick={setRedirect} />
    </BasicLayout>
  );
}
