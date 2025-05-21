import { BrowserRouter, Routes, Route } from "react-router-dom";
import HomePage from "main/pages/HomePage";
import ProfilePage from "main/pages/ProfilePage";
import AdminUsersPage from "main/pages/Admin/AdminUsersPage";

import { hasRole, useCurrentUser } from "main/utils/currentUser";

import "bootstrap/dist/css/bootstrap.css";
import "react-toastify/dist/ReactToastify.css";
import CoursesIndexPage from "main/pages/Instructors/CoursesIndexPage";
import InstructorsIndexPage from "main/pages/Admin/InstructorsIndexPage";
import AdminsIndexPage from "main/pages/Admin/AdminsIndexPage";
import CoursesIndexPage from "main/pages/Courses/CoursesIndexPage";

import InstructorsIndexPage from "main/pages/Instructors/InstructorsIndexPage";
import InstructorsCreatePage from "main/pages/Instructors/InstructorsCreatePage";

import AdminsIndexPage from "main/pages/Admins/AdminsIndexPage";
import AdminsCreatePage from "main/pages/Admins/AdminsCreatePage";

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
        {hasRole(currentUser, "ROLE_ADMIN") && (
          <>
            <Route exact path="/admin/admins" element={<AdminsIndexPage />} />
            <Route
              exact
              path="/admin/instructors"
              element={<InstructorsIndexPage />}
            />
          </>
        )}
        {hasRole(currentUser, "ROLE_INSTRUCTOR") && (
          <Route
            exact
            path="/instructor/courses"
            element={<CoursesIndexPage />}
          />
        )}
        {hasRole(currentUser, "ROLE_ADMIN") && (
          <>
            <Route
              exact
              path="/admin/admins/create"
              element={<AdminsCreatePage />}
            />
            <Route
              exact
              path="/admin/instructors/create"
              element={<InstructorsCreatePage />}
            />
          </>
        )}
      </Routes>
    </BrowserRouter>
  );
}

export default App;
