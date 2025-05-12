import { BrowserRouter, Routes, Route } from "react-router-dom";
import HomePage from "main/pages/HomePage";
import ProfilePage from "main/pages/ProfilePage";
import AdminUsersPage from "main/pages/AdminUsersPage";

import { hasRole, useCurrentUser } from "main/utils/currentUser";

import "bootstrap/dist/css/bootstrap.css";
import "react-toastify/dist/ReactToastify.css";
import SuccessfulLinkPage from "./main/pages/Courses/SuccessfulLinkPage";
import NoPermsLinkPage from "main/pages/Courses/NoPermsLinkPage";

function App() {
  const { data: currentUser } = useCurrentUser();

  return (
    <BrowserRouter>
      <Routes>
        <Route exact path="/" element={<HomePage />} />
        <Route exact path="/profile" element={<ProfilePage />} />
        {hasRole(currentUser, "ROLE_ADMIN") && (
          <Route exact path="/admin/users" element={<AdminUsersPage />} />
        )}
        <Route exact path="/courses/success/" element={<SuccessfulLinkPage />} />
        <Route exact path="/courses/noperms/" element={<NoPermsLinkPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
