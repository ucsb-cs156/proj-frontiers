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
import HomePageLoggedIn from "main/pages/HomePageLoggedIn";

function App() {
  const { data: currentUser } = useCurrentUser();

  const userRoutes = hasRole(currentUser, "ROLE_USER") ? (
    <>
      <Route exact path="/profile" element={<ProfilePage />} />
    </>
  ) : null;

  const adminRoutes = hasRole(currentUser, "ROLE_ADMIN") ? (
    <>
      <Route exact path="/admin/users" element={<AdminUsersPage />} />
      <Route exact path="/admin/admins" element={<AdminsIndexPage />} />
      <Route exact path="/instructor/courses" element={<CoursesIndexPage />} />
      <Route
        exact
        path="/instructor/courses/:id"
        element={<InstructorCourseShowPage />}
      />
      <Route
        exact
        path="/admin/instructors"
        element={<InstructorsIndexPage />}
      />
      <Route exact path="/admin/admins/create" element={<AdminsCreatePage />} />
      <Route
        exact
        path="/admin/instructors/create"
        element={<InstructorsCreatePage />}
      />
    </>
  ) : null;

  const instructorRoutes = hasRole(currentUser, "ROLE_ADMIN") ? (
    <>
      <Route exact path="/instructor/courses" element={<CoursesIndexPage />} />
      <Route
        exact
        path="/instructor/courses/:id"
        element={<InstructorCourseShowPage />}
      />
    </>
  ) : null;

  const homeRoutes = currentUser?.loggedIn ? (
    <Route path="/" element={<HomePageLoggedIn />} />
  ) : (
    <Route path="/" element={<HomePageLoggedOut />} />
  );

  return (
    <BrowserRouter>
      <Routes>
        {userRoutes}
        {adminRoutes}
        {instructorRoutes}
        {homeRoutes}
      </Routes>
    </BrowserRouter>
  );
}

export default App;
