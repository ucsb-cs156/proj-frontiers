import { BrowserRouter, Routes, Route } from "react-router-dom";
import HomePageLoggedOut from "main/pages/HomePageLoggedOut";
import ProfilePage from "main/pages/ProfilePage";
import AdminUsersPage from "main/pages/Admin/AdminUsersPage";

import { hasRole, useCurrentUser } from "main/utils/currentUser";

import "bootstrap/dist/css/bootstrap.css";
import "react-toastify/dist/ReactToastify.css";

import InstructorsIndexPage from "main/pages/Admin/InstructorsIndexPage";
import AdminsIndexPage from "main/pages/Admin/AdminsIndexPage";

import InstructorsCreatePage from "main/pages/Admin/InstructorsCreatePage";
import AdminsCreatePage from "main/pages/Admin/AdminsCreatePage";

import CoursesIndexPage from "main/pages/Instructors/CoursesIndexPage";
import InstructorCourseShowPage from "main/pages/Instructor/InstructorCourseShowPage";

function App() {
  const currentUser = useCurrentUser();

  return (
    <BrowserRouter>
      <Routes>
        <Route exact path="/" element={<HomePageLoggedOut />} />
        <Route exact path="/profile" element={<ProfilePage />} />
        {hasRole(currentUser, "ROLE_ADMIN") && (
          <Route exact path="/admin/users" element={<AdminUsersPage />} />
        )}
        {(hasRole(currentUser, "ROLE_ADMIN") ||
          hasRole(currentUser, "ROLE_INSTRUCTOR")) && (
          <>
            <Route
              exact
              path="/instructor/courses"
              element={<CoursesIndexPage />}
            />
            <Route
              exact
              path="/instructor/courses/:id"
              element={<InstructorCourseShowPage />}
            />
          </>
        )}
        {hasRole(currentUser, "ROLE_ADMIN") && (
          <>
            <Route exact path="/admin/admins" element={<AdminsIndexPage />} />
            <Route
              exact
              path="/admin/instructors"
              element={<InstructorsIndexPage />}
            />
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
