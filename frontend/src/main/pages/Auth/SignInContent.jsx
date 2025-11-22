import React from "react";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { FaMicrosoft } from "react-icons/fa";
import { FcGoogle } from "react-icons/fc";
import { Alert, Row } from "react-bootstrap";
import SignInCard from "main/components/Auth/SignInCard";
import { useSystemInfo } from "main/utils/systemInfo";
import loginProviderSchools from "main/utils/loginProviderSchools";

export default function SignInContent({
  showPromptAlert = false,
  onCardClick, // used by PromptSignInPage
}) {
  const { data: systemInfo } = useSystemInfo();

  const microsoftIcon = () => (
    <span data-testid={"SignInPage-microsoftIcon"}>
      <FaMicrosoft size={"10em"} role={"img"} />
    </span>
  );

  const googleIcon = () => (
    <span data-testid={"SignInPage-googleIcon"}>
      <FcGoogle size={"10em"} role={"img"} />
    </span>
  );

  return (
    <BasicLayout>
      {showPromptAlert && (
        <Row className="p-3">
          <Alert variant={"danger"}>
            Please sign in before accessing this page.
          </Alert>
        </Row>
      )}
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
              <>
                If you have credentials with these schools, sign in with Google
                <ul>
                  {loginProviderSchools.google.map((school, index) => (
                    <li key={index}>{school}</li>
                  ))}
                </ul>
              </>
            }
            url={systemInfo.oauthLogin}
            testid={"google"}
            onClick={onCardClick}
          />
        )}
        {systemInfo.activeDirectoryUrl && (
          <SignInCard
            Icon={microsoftIcon}
            title={"Sign in with Microsoft"}
            description={
              <>
                If you have credentials with these schools, sign in with
                Microsoft
                <ul>
                  {loginProviderSchools.microsoft.map((school, index) => (
                    <li key={index}>{school}</li>
                  ))}
                </ul>
              </>
            }
            url={systemInfo.activeDirectoryUrl}
            testid={"microsoft"}
            onClick={onCardClick}
          />
        )}
      </Row>
    </BasicLayout>
  );
}
