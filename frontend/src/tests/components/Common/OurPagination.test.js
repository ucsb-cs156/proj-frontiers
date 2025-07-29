import { fireEvent, render, screen, within } from "@testing-library/react";
import OurPagination, {
  emptyArray,
} from "main/components/Common/OurPagination";
import { useState } from "react";

const checkTestIdsInOrder = (testIds) => {
  const links = screen.getAllByTestId(/OurPagination-/);
  expect(links.length).toBe(testIds.length);

  for (var i = 0; i < links.length; i++) {
    expect(screen.getByTestId(testIds[i])).toBe(links[i]);
  }
};

const mockStateWrapper = jest.fn();

const PaginationWrapper = ({ beginActivePage, args }) => {
  const [activePage, setActivePage] = useState(beginActivePage);
  const aliasActivePage = (x) => {
    mockStateWrapper(x);
    setActivePage(x);
  };
  return (
    <OurPagination
      currentActivePage={activePage}
      updateActivePage={aliasActivePage}
      {...args}
    />
  );
};

describe("OurPagination tests", () => {
  test("emptyArray returns empty array", () => {
    expect(emptyArray()).toStrictEqual([]);
  });

  test("renders correctly for totalPages = 5 (less than or equal to 7)", async () => {
    render(<PaginationWrapper beginActivePage={1} args={{ totalPages: 5 }} />);

    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-2",
      "OurPagination-3",
      "OurPagination-4",
      "OurPagination-5",
      "OurPagination-next",
    ]);
  });

  test("renders correctly for totalPages = 7 (less than or equal to 7)", async () => {
    render(<PaginationWrapper beginActivePage={1} args={{ totalPages: 7 }} />);

    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-2",
      "OurPagination-3",
      "OurPagination-4",
      "OurPagination-5",
      "OurPagination-6",
      "OurPagination-7",
      "OurPagination-next",
    ]);
  });

  test("renders correctly for totalPages = 12 (greater than 7), initial state (activePage=1)", async () => {
    render(<PaginationWrapper beginActivePage={1} args={{ totalPages: 12 }} />);
    // Expected: 1, 2, 3, 4, 5, ..., 12
    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-2",
      "OurPagination-3",
      "OurPagination-4",
      "OurPagination-5",
      "OurPagination-right-ellipsis",
      "OurPagination-12",
      "OurPagination-next",
    ]);
  });

  test("renders correctly for totalPages = 12, activePage = 4", async () => {
    render(<PaginationWrapper beginActivePage={4} args={{ totalPages: 12 }} />);

    // Click to page 4
    fireEvent.click(screen.getByTestId("OurPagination-4"));

    // Expected: 1, 2, 3, 4, 5, ..., 12 (activePage < 5)
    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-2",
      "OurPagination-3",
      "OurPagination-4",
      "OurPagination-5",
      "OurPagination-right-ellipsis",
      "OurPagination-12",
      "OurPagination-next",
    ]);
  });

  test("renders correctly for totalPages = 12, activePage = 5 (middle range)", async () => {
    render(<PaginationWrapper beginActivePage={5} args={{ totalPages: 12 }} />);
    fireEvent.click(screen.getByTestId("OurPagination-5"));
    // Expected: 1, ..., 4, 5, 6, ..., 12
    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-left-ellipsis",
      "OurPagination-4",
      "OurPagination-5",
      "OurPagination-6",
      "OurPagination-right-ellipsis",
      "OurPagination-12",
      "OurPagination-next",
    ]);
  });

  test("renders correctly for totalPages = 12, activePage = 8 (middle range, next clicks)", async () => {
    render(<PaginationWrapper beginActivePage={1} args={{ totalPages: 12 }} />);

    const nextButton = screen.getByTestId("OurPagination-next");
    fireEvent.click(nextButton); // page 2
    fireEvent.click(nextButton); // page 3
    fireEvent.click(nextButton); // page 4
    fireEvent.click(nextButton); // page 5
    // Expected: 1, ..., 4, 5, 6, ..., 12
    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-left-ellipsis",
      "OurPagination-4",
      "OurPagination-5",
      "OurPagination-6",
      "OurPagination-right-ellipsis",
      "OurPagination-12",
      "OurPagination-next",
    ]);
    fireEvent.click(nextButton); // page 6
    // Expected: 1, ..., 5, 6, 7, ..., 12
    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-left-ellipsis",
      "OurPagination-5",
      "OurPagination-6",
      "OurPagination-7",
      "OurPagination-right-ellipsis",
      "OurPagination-12",
      "OurPagination-next",
    ]);
    fireEvent.click(nextButton); // page 7
    // Expected: 1, ..., 6, 7, 8, ..., 12
    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-left-ellipsis",
      "OurPagination-6",
      "OurPagination-7",
      "OurPagination-8",
      "OurPagination-right-ellipsis",
      "OurPagination-12",
      "OurPagination-next",
    ]);
    fireEvent.click(nextButton); // page 8
    // Expected: 1, ..., 7, 8, 9, ..., 12
    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-left-ellipsis",
      "OurPagination-7",
      "OurPagination-8",
      "OurPagination-9",
      "OurPagination-right-ellipsis",
      "OurPagination-12",
      "OurPagination-next",
    ]);
  });

  test("renders correctly for totalPages = 12, activePage = 9 (near end)", async () => {
    render(<PaginationWrapper beginActivePage={9} args={{ totalPages: 12 }} />);
    // Click to page 9 (totalPages - 3)
    // This requires multiple clicks on next or direct jump if available.
    // For simplicity, we'll simulate state by clicking next multiple times
    for (let i = 0; i < 8; i++) {
      // 1 -> 2 -> ... -> 9
      fireEvent.click(screen.getByTestId("OurPagination-next"));
    }
    // Expected: 1, ..., 8, 9, 10, 11, 12
    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-left-ellipsis",
      "OurPagination-8",
      "OurPagination-9",
      "OurPagination-10",
      "OurPagination-11",
      "OurPagination-12",
      "OurPagination-next",
    ]);
    expect(screen.queryByText("stryker was here")).not.toBeInTheDocument();
  });

  test("renders correctly for totalPages = 12, activePage = 12 (end)", async () => {
    render(
      <PaginationWrapper beginActivePage={12} args={{ totalPages: 12 }} />,
    );
    fireEvent.click(screen.getByTestId("OurPagination-12"));
    // Expected: 1, ..., 8, 9, 10, 11, 12
    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-left-ellipsis",
      "OurPagination-8",
      "OurPagination-9",
      "OurPagination-10",
      "OurPagination-11",
      "OurPagination-12",
      "OurPagination-next",
    ]);
  });

  test("navigation: prev button works correctly", async () => {
    render(<PaginationWrapper beginActivePage={1} args={{ totalPages: 12 }} />);
    // Go to page 5
    fireEvent.click(screen.getByTestId("OurPagination-5"));
    expect(
      within(screen.getByTestId("OurPagination-5")).getByText("(current)"),
    ).toBeInTheDocument();

    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-left-ellipsis",
      "OurPagination-4",
      "OurPagination-5",
      "OurPagination-6",
      "OurPagination-right-ellipsis",
      "OurPagination-12",
      "OurPagination-next",
    ]);

    const prevButton = screen.getByTestId("OurPagination-prev");
    fireEvent.click(prevButton); // Go to page 4

    // Assert that page 4 is now active
    expect(
      within(screen.getByTestId("OurPagination-4")).getByText("(current)"),
    ).toBeInTheDocument();
    expect(
      within(screen.getByTestId("OurPagination-5")).queryByText("(current)"),
    ).not.toBeInTheDocument();

    // Expected: 1, 2, 3, 4, 5, ..., 12
    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-2",
      "OurPagination-3",
      "OurPagination-4",
      "OurPagination-5",
      "OurPagination-right-ellipsis",
      "OurPagination-12",
      "OurPagination-next",
    ]);
  });

  test("navigation: clicking page 1 from a middle page", async () => {
    render(<PaginationWrapper beginActivePage={1} args={{ totalPages: 12 }} />);
    // Go to page 5
    fireEvent.click(screen.getByTestId("OurPagination-5"));
    checkTestIdsInOrder([
      // Page 5
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-left-ellipsis",
      "OurPagination-4",
      "OurPagination-5",
      "OurPagination-6",
      "OurPagination-right-ellipsis",
      "OurPagination-12",
      "OurPagination-next",
    ]);

    fireEvent.click(screen.getByTestId("OurPagination-1"));
    // Expected: 1, 2, 3, 4, 5, ..., 12
    checkTestIdsInOrder([
      "OurPagination-prev",
      "OurPagination-1",
      "OurPagination-2",
      "OurPagination-3",
      "OurPagination-4",
      "OurPagination-5",
      "OurPagination-right-ellipsis",
      "OurPagination-12",
      "OurPagination-next",
    ]);
  });

  test("checks active class on buttons", async () => {
    render(<PaginationWrapper beginActivePage={1} args={{ totalPages: 5 }} />);

    // Initial state: page 1 is active
    expect(
      within(screen.getByTestId("OurPagination-1")).getByText("(current)"),
    ).toBeInTheDocument();
    expect(
      within(screen.getByTestId("OurPagination-2")).queryByText("(current)"),
    ).not.toBeInTheDocument();

    fireEvent.click(screen.getByTestId("OurPagination-next")); // page 2

    // After re-render: page 2 should be active. Re-query elements.
    expect(
      within(screen.getByTestId("OurPagination-1")).queryByText("(current)"),
    ).not.toBeInTheDocument();
    expect(
      within(screen.getByTestId("OurPagination-2")).getByText("(current)"),
    ).toBeInTheDocument();
  });
});
