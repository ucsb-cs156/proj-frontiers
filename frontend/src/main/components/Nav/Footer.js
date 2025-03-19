import { Container } from "react-bootstrap";

export default function Footer() {
  return (
    <footer className="bg-light pt-3 pt-md-4 pb-4 pb-md-5" data-testid="Footer">
      <Container>
        <p>
          This is the MVP for the Frontiers project. See the repo on{" "}
          <a href="https://github.com/ucsb-cs156/proj-frontiers">
            Github.
          </a>
        </p>
      </Container>
    </footer>
  );
}
