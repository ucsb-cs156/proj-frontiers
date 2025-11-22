import React from "react";
import { useLocation } from "react-router";
import SignInContent from "main/components/Auth/SignInContent";

export default function PromptSignInPage() {
  const location = useLocation();

  const setRedirect = () => {
    sessionStorage.setItem("redirect", location.pathname);
  };

  return (
    <SignInContent
      showPromptAlert={true}
      onCardClick={setRedirect}
    />
  );
}
