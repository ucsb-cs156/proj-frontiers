import { NavDropdown } from "react-bootstrap";
import { Link } from "react-router";

export default function HelpMenu() {
  return (
    <>
      <NavDropdown
        title="Help"
        id="appnavbar-help-dropdown"
        data-testid="appnavbar-help-dropdown"
        className="narrow-dropdown" // Add the custom class
        align="end" // Align the menu to the right
      >
        <NavDropdown.Item as={Link} to="/help/about">
          About Frontiers
        </NavDropdown.Item>
        <NavDropdown.Item as={Link} to="/help/csv">
          CSV Upload/Download Formats
        </NavDropdown.Item>
      </NavDropdown>
    </>
  );
}
