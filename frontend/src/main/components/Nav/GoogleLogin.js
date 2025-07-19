import { Button, Navbar } from "react-bootstrap";
import { Link } from "react-router-dom";

export default function GoogleLogin({ currentUser, handleLogin, doLogout }) {
  return (
    <>
      {currentUser && currentUser.loggedIn ? (
        <>
          <Navbar.Text className="me-3" as={Link} to="/profile">
            Welcome, {currentUser.root.user.email}
          </Navbar.Text>
          <Button onClick={doLogout}>Log Out</Button>
        </>
      ) : (
        <Button onClick={handleLogin}>Log In</Button>
      )}
    </>
  );
}
