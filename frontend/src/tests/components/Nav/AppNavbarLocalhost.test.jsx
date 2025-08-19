import { render, screen } from "@testing-library/react";
import AppNavbarLocalhost from "../../../main/components/Nav/AppNavbarLocalhost";

describe("AppNavbarLocalhost tests", () => {
  test("renders correctly", async () => {
    render(<AppNavbarLocalhost />);
    await screen.findByText(/Running on /);
  });
});
