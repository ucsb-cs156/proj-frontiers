import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { FaGithubSquare } from "react-icons/fa";
import { useSystemInfo } from "main/utils/systemInfo";
import { Row } from "react-bootstrap";
import SignInCard from "main/components/Auth/SignInCard";
import { useLocation } from "react-router-dom";

export default function HomePageConnectGithub() {
  const githubIcon = () => {
    return (
      <span data-testid={"HomePageConnectGithub-githubIcon"}>
        <FaGithubSquare size={"10em"} role={"img"} />
      </span>
    );
  };
  const location = useLocation();

  const setRedirect = () => {
    sessionStorage.setItem("redirect", location.pathname);
  };

  const { data: systemInfo } = useSystemInfo();

  var githubOauthLogin =
    systemInfo?.githubOauthLogin || "/oauth2/authorization/github";

  return (
    <BasicLayout>
      <Row
        xs={1}
        md={2}
        className={"g-5 d-flex gap-5 justify-content-center align-items-center"}
        data-testid={"HomePageConnectGithub-cardDisplay"}
      >
        <SignInCard
          Icon={githubIcon}
          title={"Sign in with Github"}
          description={
            "Please connect your account with a GitHub account to continue to Frontiers."
          }
          url={githubOauthLogin}
          testid={"github"}
          onClick={setRedirect}
        />
      </Row>
    </BasicLayout>
  );
}
