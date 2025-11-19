import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import SignInCardDisplay from "main/components/Auth/SignInCardDisplay";
import { useLocation } from "react-router";

export default function PromptSignInPage() {
  const location = useLocation();

  const setRedirect = () => {
    sessionStorage.setItem("redirect", location.pathname);
  };

  return (
    <BasicLayout>
      <SignInCardDisplay
        alertMessage="Please sign in before accessing this page."
        onClick={setRedirect}
      />
    </BasicLayout>
  );
}
