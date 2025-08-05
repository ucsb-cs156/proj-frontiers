import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { FaMicrosoft } from "react-icons/fa";
import { FcGoogle } from "react-icons/fc";
import { Alert, Row } from "react-bootstrap";
import SignInCard from "main/components/Auth/SignInCard";
import { useSystemInfo } from "main/utils/systemInfo";
import { useLocation } from "react-router";

export default function PromptSignInPage() {
  const microsoftIcon = () => {
    return (
      <span data-testid={"SignInPage-microsoftIcon"}>
        <FaMicrosoft size={"10em"} role={"img"} />
      </span>
    );
  };

  const { data: systemInfo } = useSystemInfo();

  const googleIcon = () => {
    return (
      <span data-testid={"SignInPage-googleIcon"}>
        <FcGoogle size={"10em"} role={"img"} />
      </span>
    );
  };

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
      <Row
        xs={1}
        md={2}
        className={"g-5 d-flex gap-5 justify-content-center align-items-center"}
        data-testid={"SignInPage-cardDisplay"}
      >
        {systemInfo.oauthLogin && (
          <SignInCard
            Icon={googleIcon}
            title={"Sign in with Google"}
            description={
              "If you have Chico State or University of California-Santa Barbara login credentials, sign in with Google"
            }
            url={systemInfo.oauthLogin}
            testid={"google"}
            onClick={setRedirect}
          />
        )}
        {systemInfo.activeDirectoryUrl && (
          <SignInCard
            Icon={microsoftIcon}
            title={"Sign in with Microsoft"}
            description={
              "If you have Oregon State University login credentials, sign in with Microsoft."
            }
            url={systemInfo.activeDirectoryUrl}
            testid={"microsoft"}
            onClick={setRedirect}
          />
        )}
      </Row>
    </BasicLayout>
  );
}
