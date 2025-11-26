import React from "react";
import { useLocation } from "react-router";
import { Row, Alert } from "react-bootstrap";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import SignInContent from "main/pages/Auth/SignInContent.jsx";

export default function PromptSignInPage() {
  const location = useLocation();

  const setRedirect = () => {
    sessionStorage.setItem("redirect", location.pathname);
  };

  return (
    <BasicLayout>
      <Row className="p-3">
        <Alert variant="danger">
          Please sign in before accessing this page.
        </Alert>
      </Row>
      <SignInContent onCardClick={setRedirect} />
    </BasicLayout>
  );
}
