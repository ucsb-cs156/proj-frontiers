import React from "react";
import { FaMicrosoft } from "react-icons/fa";
import { FcGoogle } from "react-icons/fc";
import { Row } from "react-bootstrap";
import SignInCard from "main/components/Auth/SignInCard";
import { useSystemInfo } from "main/utils/systemInfo";
import loginProviderSchools from "main/utils/loginProviderSchools";

const SignInOptions = ({ onSignIn }) => {
  const { data: systemInfo } = useSystemInfo();

  const microsoftIcon = () => {
    return (
      <span data-testid={"SignInOptions-microsoftIcon"}>
        <FaMicrosoft size={"10em"} role={"img"} />
      </span>
    );
  };

  const googleIcon = () => {
    return (
      <span data-testid={"SignInOptions-googleIcon"}>
        <FcGoogle size={"10em"} role={"img"} />
      </span>
    );
  };

  return (
    <Row
      xs={1}
      md={2}
      className={"g-5 d-flex gap-5 justify-content-center align-items-center"}
      data-testid={"SignInOptions-cardDisplay"}
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
          onClick={onSignIn}
        />
      )}
      {systemInfo.activeDirectoryUrl && (
        <SignInCard
          Icon={microsoftIcon}
          title={"Sign in with Microsoft"}
          description={
            <>
              If you have credentials with these schools, sign in with Microsoft
              <ul>
                {loginProviderSchools.microsoft.map((school, index) => (
                  <li key={index}>{school}</li>
                ))}
              </ul>
            </>
          }
          url={systemInfo.activeDirectoryUrl}
          testid={"microsoft"}
          onClick={onSignIn}
        />
      )}
    </Row>
  );
};

export default SignInOptions;
