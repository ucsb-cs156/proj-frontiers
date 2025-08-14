import { render, screen } from "@testing-library/react";
import GithubSettingIcon from "main/components/Common/GithubSettingIcon";

describe("GithubSettingIcon tests", () => {
  test("renders correctly with defaults", () => {
    render(<GithubSettingIcon />);
           
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
    expect(
      screen.getByTestId("GithubSettingIcon-github-icon"),
    ).toBeInTheDocument();
    expect(gearIcon).toHaveStyle({
      position: "relative",
      top: "0px",
    });
    expect(gearIcon).toHaveStyle({
      left: "0px",
      transform: "translate(-15%, 60%)",
    });
  });
  test("renders correctly with custom props", () => {
    render(<GithubSettingIcon size={32} gearColor="red" githubColor="blue" />);
    expect(
      screen.getByTestId("GithubSettingIcon-github-icon"),
    ).toBeInTheDocument();
    const settingsIcon = screen.getByTestId("GithubSettingIcon-settings-icon");
    expect(settingsIcon).toBeInTheDocument();
    expect(settingsIcon).toHaveStyle({
      position: "relative",
      top: "0px",
    });
    expect(settingsIcon).toHaveStyle({
      left: "0px",
      transform: "translate(-15%, 60%)",
    });
  });
});
