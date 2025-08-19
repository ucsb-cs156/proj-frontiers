import { hasRole } from "main/utils/currentUser";
import AccessDeniedPage from "main/pages/Auth/AccessDeniedPage";
import PromptSignInPage from "main/pages/Auth/PromptSignInPage";
import LoadingPage from "main/pages/LoadingPage";

export default function ProtectedPage({ component, currentUser, enforceRole }) {
  if (currentUser.initialData) {
    return <LoadingPage />;
  }
  if (hasRole(currentUser, enforceRole)) {
    return <>{component}</>;
  } else if (!currentUser.loggedIn) {
    return <PromptSignInPage />;
  } else {
    return <AccessDeniedPage />;
  }
}
