import { Button, Navbar } from "react-bootstrap";
import { Link } from "react-router";

export default function GithubLogin({ currentUser, systemInfo }) {
  var githubOauthLogin =
    systemInfo?.githubOauthLogin || "/oauth2/authorization/github";
  if (!currentUser || !currentUser.loggedIn) {
    return <span data-testid="GithubLogin-logged-out" />;
  }
  if (currentUser.root.user.githubLogin) {
    return (
      <>
        <Navbar.Text className="me-3" as={Link} to="/profile">
          Github: {currentUser.root.user.githubLogin}
        </Navbar.Text>
      </>
    );
  }
  return (
    <>
      <Button href={githubOauthLogin}>Connect Github</Button>
    </>
  );
}
