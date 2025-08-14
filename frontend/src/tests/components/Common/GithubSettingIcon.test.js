import { render, screen } from "@testing-library/react";
import GithubSettingIcon from "main/components/Common/GithubSettingIcon";

describe("GithubSettingIcon tests", () => {
  test("Renders correctly", async () => {
    render(<GithubSettingIcon />);
    expect(screen.getByTestId("GithubSettingIcon")).toBeInTheDocument();
  });

  it("renders with correct defaults", async () => {
    render(<GithubSettingIcon />);
    // Check main container
    const container = screen.getByTestId("GithubSettingIcon");
    expect(container).toBeInTheDocument();

    // Check Github icon
    const githubIcon = screen.getByTestId("GithubSettingIcon-github-icon");
    expect(githubIcon).toBeInTheDocument();
    expect(githubIcon).toHaveAttribute("color", "black");

    // Check Gear icon
    const gearIcon = screen.getByTestId("GithubSettingIcon-settings-icon");
    expect(gearIcon).toBeInTheDocument();
    expect(gearIcon).toHaveAttribute("color", "blue");
  });
});
