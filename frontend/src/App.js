import { BrowserRouter, Route, Routes } from "react-router";
import ProfilePage from "main/pages/ProfilePage";
import AdminUsersPage from "main/pages/Admin/AdminUsersPage";

import { hasRole, useCurrentUser } from "main/utils/currentUser";

import "bootstrap/dist/css/bootstrap.css";
import "react-toastify/dist/ReactToastify.css";

import InstructorsIndexPage from "main/pages/Admin/InstructorsIndexPage";
import AdminsIndexPage from "main/pages/Admin/AdminsIndexPage";

import InstructorsCreatePage from "main/pages/Admin/InstructorsCreatePage";
import AdminsCreatePage from "main/pages/Admin/AdminsCreatePage";

import CoursesIndexPage from "main/pages/Admin/CoursesIndexPage";
import AdminJobsPage from "main/pages/Admin/AdminJobsPage";
import InstructorCourseShowPage from "main/pages/Instructor/InstructorCourseShowPage";
import HomePageLoggedIn from "main/pages/HomePageLoggedIn";
import HomePageConnectGithub from "main/pages/HomePageConnectGithub";
import SignInSuccessPage from "main/pages/Auth/SignInSuccessPage";
import ProtectedPage from "main/pages/Auth/ProtectedPage";
import HomePageLoggedOut from "main/pages/HomePageLoggedOut";
import SignInPage from "main/pages/Auth/SignInPage";
import NotFoundPage from "main/pages/Auth/NotFoundPage";
import HelpAboutPage from "main/pages/Help/HelpAboutPage";
import HelpCsvPage from "main/pages/Help/HelpCsvPage";

function App() {
  const currentUser = useCurrentUser();

  if (currentUser.loggedIn && !hasRole(currentUser, "ROLE_GITHUB")) {
    return (
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomePageConnectGithub />} />
          <Route path="/help/about" element={<HelpAboutPage />} />
          <Route path="/help/csv" element={<HelpCsvPage />} />
          <Route path="*" element={<HomePageConnectGithub />} />
          <Route path="/login/success" element={<SignInSuccessPage />} />
        </Routes>
      </BrowserRouter>
    );
  }

  const homePage = currentUser.loggedIn ? (
    <HomePageLoggedIn />
  ) : (
    <HomePageLoggedOut />
  );

  return (
    <BrowserRouter>
      <Routes>
        <Route path="*" element={<NotFoundPage />} />
        <Route path="/login" element={<SignInPage />} />
        <Route path="/" element={homePage} />
        <Route path="/help/about" element={<HelpAboutPage />} />
        <Route path="/help/csv" element={<HelpCsvPage />} />
        <Route
          path="/profile"
          element={
            <ProtectedPage
              component={<ProfilePage />}
              enforceRole={"ROLE_USER"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/login/success"
          element={
            <ProtectedPage
              component={<SignInSuccessPage />}
              enforceRole={"ROLE_USER"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/admin/users"
          element={
            <ProtectedPage
              component={<AdminUsersPage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/admin/admins"
          element={
            <ProtectedPage
              component={<AdminsIndexPage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/admin/instructors"
          element={
            <ProtectedPage
              component={<InstructorsIndexPage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/admin/admins/create"
          element={
            <ProtectedPage
              component={<AdminsCreatePage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/admin/courses"
          element={
            <ProtectedPage
              component={<CoursesIndexPage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/admin/jobs"
          element={
            <ProtectedPage
              component={<AdminJobsPage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/instructor/courses/:id"
          element={
            <ProtectedPage
              component={<InstructorCourseShowPage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
        <Route
          path="/admin/instructors/create"
          element={
            <ProtectedPage
              component={<InstructorsCreatePage />}
              enforceRole={"ROLE_ADMIN"}
              currentUser={currentUser}
            />
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
