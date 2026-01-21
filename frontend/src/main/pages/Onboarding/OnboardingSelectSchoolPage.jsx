import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { Typeahead } from "react-bootstrap-typeahead";
import { schoolToProvider } from "main/utils/loginProviderSchools";
import React, { useState } from "react";
import { Row, Button, Col, ProgressBar } from "react-bootstrap";
import { useSystemInfo } from "main/utils/systemInfo";
import { FaMicrosoft } from "react-icons/fa";
import { FcGoogle } from "react-icons/fc";
import SignInCard from "main/components/Auth/SignInCard";

export default function OnboardingSelectSchoolPage() {
  const [selectedProvider, setSelectedProvider] = useState(null);
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

  const notSetUp = () => {
    return (
      <p>
        We&#39;re sorry, your login provider isn&#39;t available at this time.
        Please contact your instructor.
      </p>
    );
  };

  const setRedirect = () => {
    sessionStorage.setItem("redirect", "/onboarding/courses");
  };

  const renderProviderSignIn = () => {
    switch (selectedProvider) {
      case "microsoft": {
        return (
          <>
            {systemInfo.activeDirectoryUrl ? (
              <SignInCard
                Icon={microsoftIcon}
                title={"Sign in with Microsoft"}
                description={
                  <>
                    Please sign in with your university-associated account via
                    Microsoft.
                  </>
                }
                url={systemInfo.activeDirectoryUrl}
                testid={"microsoft"}
                onClick={setRedirect}
              />
            ) : (
              notSetUp()
            )}
          </>
        );
      }
      case "google": {
        return (
          <>
            {systemInfo.oauthLogin ? (
              <SignInCard
                Icon={googleIcon}
                title={"Sign in with Google"}
                description={
                  <>
                    Please sign in with your university-associated account via
                    Google.
                  </>
                }
                url={systemInfo.oauthLogin}
                testid={"google"}
                onClick={setRedirect}
              />
            ) : (
              notSetUp()
            )}
          </>
        );
      }
    }
  };

  return (
    <BasicLayout>
      <Row>
        <Col>
          <h1>Welcome to Frontiers!</h1>
          <p>Let&#39;s get started.</p>
          <p>
            We need a bit of information from you. Please select the school you
            have credentials with:
          </p>
          <Typeahead
            options={schoolToProvider}
            placeholder="Start typing to select a school..."
            labelKey="schoolName"
            inputProps={{
              "aria-label": "Choose a school",
              "data-testid": "SelectSchool-typeahead",
            }}
            onChange={(selected) => {
              setSelectedProvider(selected[0]?.provider);
            }}
          />
        </Col>
        <Col className="text-center justify-content-center align-items-center d-flex">
          <>{selectedProvider && renderProviderSignIn()}</>
        </Col>
      </Row>
      <Row className="mt-auto pb-3">
        <Col>
          <ProgressBar now={20} />
        </Col>
      </Row>
    </BasicLayout>
  );
}
