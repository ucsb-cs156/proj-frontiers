import React from "react";
import { useLocation } from "react-router";
import SignInContent from "main/pages/Auth/SignInContent.jsx";

export default function PromptSignInPage() {
  const location = useLocation();

  const setRedirect = () => {
    sessionStorage.setItem("redirect", location.pathname);
  };

  return <SignInContent showPromptAlert={true} onCardClick={setRedirect} />;
}
