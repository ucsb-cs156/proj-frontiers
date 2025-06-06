import { Button, Navbar } from "react-bootstrap";
import { Link } from "react-router-dom";

export default function GithubLogin({ currentUser, systemInfo }) {
  var githubOauthLogin =
    systemInfo?.githubOauthLogin || "/oauth2/authorization/github";
  if (!currentUser || !currentUser.loggedIn) {
    return <span data-testid="GithubLogin-logged-out" />;
  }

  // Add this null check for currentUser.root.user
  if (!currentUser.root?.user) {
    return <span data-testid="GithubLogin-loading" />;
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
