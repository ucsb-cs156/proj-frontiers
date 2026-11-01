import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";

import EduDoraTabComponent from "main/components/TabComponent/EduDoraTabComponent";

describe("EduDoraTabComponent tests", () => {
  test("renders the placeholder text", () => {
    render(<EduDoraTabComponent testIdPrefix="TestPrefix" />);

    const tab = screen.getByTestId("TestPrefix-edudora-tab-component");
    expect(tab).toHaveTextContent("EduDora features coming soon");
  });
});
