import { Row } from "react-bootstrap";
import SignInCard from "main/components/Auth/SignInCard";
import { useSystemInfo } from "main/utils/systemInfo";
import loginProviderSchools from "main/utils/loginProviderSchools";
import { FaMicrosoft } from "react-icons/fa";
import { FcGoogle } from "react-icons/fc";

export default function SignInProviders({ onClick }) {
  const microsoftIcon = () => {
    return (
      <span data-testid="SignInProvider-microsoftIcon">
        <FaMicrosoft size="10em" role="img" />
      </span>
    );
  };

  const { data: systemInfo } = useSystemInfo();

  const googleIcon = () => {
    return (
      <span data-testid="SignInProvider-googleIcon">
        <FcGoogle size="10em" role="img" />
      </span>
    );
  };

  return (
    <Row
      xs={1}
      md={2}
      className="g-5 d-flex gap-5 justify-content-center align-items-center"
      data-testid="SignInProvider-cardDisplay"
    >
      {systemInfo.oauthLogin && (
        <SignInCard
          Icon={googleIcon}
          title="Sign in with Google"
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
          testid="google"
          onClick={onClick}
        />
      )}
      {systemInfo.activeDirectoryUrl && (
        <SignInCard
          Icon={microsoftIcon}
          title="Sign in with Microsoft"
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
          testid="microsoft"
          onClick={onClick}
        />
      )}
    </Row>
  );
}
