import { render, screen } from "@testing-library/react";
import Footer from "main/components/Nav/Footer";

describe("Footer tests", () => {
  test("renders correctly", async () => {
    render(<Footer />);
    const expectedText =
      "This is the MVP for the Frontiers project. See the repo on Github.";
    expect(screen.getByTestId("Footer").textContent).toBe(expectedText);
    // await screen.findByText(
    //   /This is a sample webapp using React with a Spring Boot backend./,
    // );
    // await screen.findByText(/source code on/);
    // const githubLink = screen.getByText("Github");
    // expect(githubLink).toBeInTheDocument();
  });
});
