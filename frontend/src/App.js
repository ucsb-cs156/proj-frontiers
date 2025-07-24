import { BrowserRouter, Routes, Route } from "react-router";
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
import LoadingPage from "main/pages/LoadingPage";
import SignInPage from "main/pages/Auth/SignInPage";

function App() {
  const currentUserData = useCurrentUser();

  if (!currentUserData) {
    return (
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomePageLoggedOut />} />
          <Route path="*" element={<HomePageLoggedOut />} />
          <Route exact path="/login" element={<SignInPage />} />
        </Routes>
      </BrowserRouter>
    );
  }

  if (currentUserData.initialData) {
    return (
      <BrowserRouter>
        <Routes>
          <Route path="*" element={<LoadingPage />} />
          <Route exact path="/login" element={<SignInPage />} />
        </Routes>
      </BrowserRouter>
    );
  }

  if (!currentUserData.loggedIn) {
    return (
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomePageLoggedOut />} />
          <Route path="*" element={<HomePageLoggedOut />} />
          <Route exact path="/login" element={<SignInPage />} />
        </Routes>
      </BrowserRouter>
    );
  }

  const currentUser = currentUserData;

  const userRoutes = hasRole(currentUser, "ROLE_USER") ? (
    <>
      <Route path="/profile" element={<ProfilePage />} />
      <Route exact path="/login" element={<SignInPage />} />
    </>
  ) : null;

  const adminRoutes = hasRole(currentUser, "ROLE_ADMIN") ? (
    <>
      <Route path="/admin/users" element={<AdminUsersPage />} />
      <Route path="/admin/admins" element={<AdminsIndexPage />} />
      <Route path="/instructor/courses" element={<CoursesIndexPage />} />
      <Route
        path="/instructor/courses/:id"
        element={<InstructorCourseShowPage />}
      />
      <Route path="/admin/instructors" element={<InstructorsIndexPage />} />
      <Route path="/admin/admins/create" element={<AdminsCreatePage />} />
      <Route
        path="/admin/instructors/create"
        element={<InstructorsCreatePage />}
      />
    </>
  ) : null;

  const instructorRoutes = hasRole(currentUser, "ROLE_INSTRUCTOR") ? (
    <>
      <Route path="/instructor/courses" element={<CoursesIndexPage />} />
      <Route
        path="/instructor/courses/:id"
        element={<InstructorCourseShowPage />}
      />
    </>
  ) : null;

  const homeRoutes =
    hasRole(currentUser, "ROLE_ADMIN") ||
    hasRole(currentUser, "ROLE_INSTRUCTOR") ||
    hasRole(currentUser, "ROLE_USER") ? (
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
