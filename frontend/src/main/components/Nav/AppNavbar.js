import { Container, Nav, Navbar, NavDropdown } from "react-bootstrap";
import { Link, useLocation, useNavigate } from "react-router";
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
  const location = useLocation();
  const handleSignIn = () => {
    sessionStorage.setItem("redirect", location.pathname);
    navigation("/login");
  };
  return (
    <>
      {(currentUrl.startsWith("http://localhost:3000") ||
        currentUrl.startsWith("http://127.0.0.1:3000")) && (
        <AppNavbarLocalhost url={currentUrl} />
      )}
      <Navbar
        expand="md"
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

          <>
            {/* be sure that each NavDropdown has a unique id and data-testid  */}
          </>

          <Navbar.Collapse className="justify-content-between">
            <Nav className="mr-auto">
              {systemInfo?.showSwaggerUILink && (
                <>
                  <Nav.Link href="/swagger-ui/index.html">Swagger</Nav.Link>
                </>
              )}
              {systemInfo?.springH2ConsoleEnabled && (
                <>
                  <Nav.Link href="/h2-console">H2Console</Nav.Link>
                </>
              )}
              {hasRole(currentUser, "ROLE_ADMIN") && (
                <NavDropdown
                  title="Admin"
                  id="appnavbar-admin-dropdown"
                  data-testid="appnavbar-admin-dropdown"
                >
                  <NavDropdown.Item as={Link} to="/admin/users">
                    Users
                  </NavDropdown.Item>
                  <NavDropdown.Item as={Link} to="/admin/admins">
                    Admins
                  </NavDropdown.Item>
                  <NavDropdown.Item as={Link} to="/admin/instructors">
                    Instructors
                  </NavDropdown.Item>
                  <NavDropdown.Item as={Link} to="/admin/courses">
                    Courses
                  </NavDropdown.Item>
                </NavDropdown>
              )}
            </Nav>
            <Nav className="ml-auto">
              <NavDropdown
                title="Help"
                id="appnavbar-help-dropdown"
                data-testid="appnavbar-help-dropdown"
              >
                <NavDropdown.Item as={Link} to="/help/about">
                  About Frontiers
                </NavDropdown.Item>
                <NavDropdown.Item as={Link} to="/help/csv">
                  CSV Upload/Download Formats
                </NavDropdown.Item>
              </NavDropdown>
            </Nav>
            {hasRole(currentUser, "ROLE_GITHUB") && (
              <Nav className="ml-auto">
                <GithubLogin
                  currentUser={currentUser}
                  systemInfo={systemInfo}
                />
              </Nav>
            )}
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
