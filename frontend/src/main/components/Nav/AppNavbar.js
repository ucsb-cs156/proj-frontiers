import { Container, Nav, Navbar, NavDropdown } from "react-bootstrap";
import { Link, useNavigate } from "react-router";
import { hasRole } from "main/utils/currentUser";
import AppNavbarLocalhost from "main/components/Nav/AppNavbarLocalhost";
import GoogleLogin from "main/components/Nav/GoogleLogin";
import GithubLogin from "main/components/Nav/GithubLogin";

export default function AppNavbar({
  currentUser,
  systemInfo,
  doLogout,
  currentUrl = window.location.href,
}) {
  const navigation = useNavigate();
  const handleSignIn = () => {
    navigation("/login");
  };
  return (
    <>
      {(currentUrl.startsWith("http://localhost:3000") ||
        currentUrl.startsWith("http://127.0.0.1:3000")) && (
        <AppNavbarLocalhost url={currentUrl} />
      )}
      <Navbar
        expand="xl"
        variant="dark"
        bg="dark"
        sticky="top"
        data-testid="AppNavbar"
      >
        <Container>
          <Navbar.Brand as={Link} to="/">
            Frontiers
          </Navbar.Brand>

          <Navbar.Toggle />

          <Nav className="me-auto">
            {systemInfo?.springH2ConsoleEnabled && (
              <>
                <Nav.Link href="/h2-console">H2Console</Nav.Link>
              </>
            )}
            {systemInfo?.showSwaggerUILink && (
              <>
                <Nav.Link href="/swagger-ui/index.html">Swagger</Nav.Link>
              </>
            )}
          </Nav>

          <>
            {/* be sure that each NavDropdown has a unique id and data-testid  */}
          </>

          <Navbar.Collapse className="justify-content-between">
            <Nav className="mr-auto">
              {hasRole(currentUser, "ROLE_ADMIN") && (
                <NavDropdown
                  title="Admin"
                  id="appnavbar-admin-dropdown"
                  data-testid="appnavbar-admin-dropdown"
                >
                  <NavDropdown.Item href="/admin/users">Users</NavDropdown.Item>
                  <NavDropdown.Item href="/admin/admins">
                    Admins
                  </NavDropdown.Item>
                  <NavDropdown.Item href="/admin/instructors">
                    Instructors
                  </NavDropdown.Item>
                </NavDropdown>
              )}
              {hasRole(currentUser, "ROLE_INSTRUCTOR") && (
                <NavDropdown
                  title="Instructor"
                  id="appnavbar-instructor-dropdown"
                  data-testid="appnavbar-instructor-dropdown"
                >
                  <NavDropdown.Item href="/instructor/courses">
                    Courses
                  </NavDropdown.Item>
                </NavDropdown>
              )}
            </Nav>
            <Nav className="ml-auto">
              <GithubLogin currentUser={currentUser} systemInfo={systemInfo} />
            </Nav>
            <Nav className="ml-auto">
              <GoogleLogin
                currentUser={currentUser}
                handleLogin={handleSignIn}
                doLogout={doLogout}
              />
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>
    </>
  );
}
