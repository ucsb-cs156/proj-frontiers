import { Button, Navbar } from "react-bootstrap";
import { Link } from "react-router-dom";

export default function GoogleLogin({ currentUser, systemInfo, doLogout }) {
  var oauthLogin = systemInfo?.oauthLogin || "/oauth2/authorization/google";
  return (
    <>
      {currentUser && currentUser.loggedIn ? (
        <>
          <Navbar.Text className="me-3" as={Link} to="/profile">
            Welcome, {currentUser.root.user.email}
          </Navbar.Text>
          <Button onClick={doLogout}>Log Out</Button>
        </>
      ) : (<span>
        <Button style={{margin: 10}} href={oauthLogin}>Log In</Button>
        <Button style={{margin: 10}} href="/oauth2/authorization/azure-dev">Log In With Microsoft</Button>
        </span>
      )}
    </>
  );
}
