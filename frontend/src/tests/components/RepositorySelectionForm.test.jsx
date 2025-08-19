import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter as Router } from "react-router";

import RepositorySelectionForm from "main/components/RepositorySelectionForm";
import collectionNames from "fixtures/collectionNames";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const mockedNavigate = jest.fn();
const queryClient = new QueryClient();

jest.mock("react-router", () => ({
  ...jest.requireActual("react-router"),
  useNavigate: () => mockedNavigate,
}));

describe("RosterStudentForm tests", () => {
  afterEach(() => {
    queryClient.clear();
  });

  const expectedHeaders = [
    "Collection Name",
    "GitHub Repository or Organization URL",
  ];

  test("renders correctly", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(/Collection Name/)).toBeInTheDocument();

    expectedHeaders.forEach((headerText) => {
      const header = screen.getByText(headerText);
      expect(header).toBeInTheDocument();
    });

    const urlInput = screen.getByTestId("URL-input");
    expect(urlInput).toHaveValue("");

    const nameInput = screen.getByTestId("collection-name-input");
    expect(nameInput).toHaveValue("");

    const messageURLContainer = screen.getByTestId("github-url-message");
    expect(messageURLContainer).toBeEmptyDOMElement();

    const messageNameContainer = screen.getByTestId("collection-name-message");
    expect(messageNameContainer).toBeEmptyDOMElement();
  });
  test("uses empty collections when no collections is passed in", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("collection-name-input");

    fireEvent.change(urlInput, { target: { value: "Stryker was here" } });

    expect(screen.queryByTestId("name-error-message")).not.toBeInTheDocument();
    expect(
      screen.queryByText(/Collection name already exists/),
    ).not.toBeInTheDocument();
  });
  test("correct error message displayed when input is empty for onBlurURL", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );

    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, { target: { value: "" } });
    fireEvent.blur(urlInput);

    expect(screen.getByTestId("url-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(/GitHub repository or organization URL is required/),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(
        /Please enter a valid GitHub repository or organization URL/,
      ),
    ).not.toBeInTheDocument();
  });
  test("shows correct error message when URL input is invalid (not correctly formatted) onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );

    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, { target: { value: "a" } });
    fireEvent.blur(urlInput);

    expect(screen.getByTestId("url-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(
        /Please enter a valid GitHub repository or organization URL/,
      ),
    ).toBeInTheDocument();
  });
  test("shows correct error message when URL input is empty onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );

    const urlInput = screen.getByTestId("URL-input");

    fireEvent.blur(urlInput);

    expect(screen.getByTestId("url-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(/GitHub repository or organization URL is required/),
    ).toBeInTheDocument();
    expect(screen.getByTestId("url-error-message")).toHaveStyle({
      color: "red",
    });
  });
  test("shows correct error message when URL input is invalid (only includes spaces) onBlurURL", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );

    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, { target: { value: " " } });
    fireEvent.blur(urlInput);

    await waitFor(() => {
      expect(screen.getByTestId("url-error-message")).toBeInTheDocument();
    });
    expect(
      screen.getByText(/GitHub repository or organization URL is required/),
    ).toBeInTheDocument();
  });
  test("does not show an error message when URL input is valid and has spaces onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );

    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, {
      target: { value: "https://github.com/user/repo " },
    });
    fireEvent.blur(urlInput);

    expect(screen.queryByTestId("url-error-message")).not.toBeInTheDocument();
    expect(screen.getByTestId("url-success-message")).toBeInTheDocument();

    const verifiedIcon = screen.getByTestId("url-success-icon");
    const successMessage = screen.getByTestId("url-success-message");
    expect(verifiedIcon).toHaveStyle({ color: "green" });
    expect(successMessage.textContent).toBe("Verified ");
    expect(successMessage).toHaveStyle({ color: "green" });
  });
  test("rejects URLs missing https protocol for onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, {
      target: { value: "http://github.com/user/repo" },
    }); // http vs https
    fireEvent.blur(urlInput);

    expect(screen.getByTestId("url-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(
        /Please enter a valid GitHub repository or organization URL/,
      ),
    ).toBeInTheDocument();
    expect(screen.getByTestId("url-error-message")).toHaveStyle({
      color: "red",
    });
  });
  test("rejects URL with wrong domain for onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, {
      target: { value: "http://git.com/user/repo" },
    });
    fireEvent.blur(urlInput);

    expect(screen.getByTestId("url-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(
        /Please enter a valid GitHub repository or organization URL/,
      ),
    ).toBeInTheDocument();
  });
  test("accepts URL with just org for onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, { target: { value: "https://github.com/org" } });
    fireEvent.blur(urlInput);

    expect(screen.getByTestId("url-success-message")).toBeInTheDocument();
    expect(screen.getByText(/Verified/)).toBeInTheDocument();
  });
  test("accepts URL with org and repo for onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, {
      target: { value: "https://github.com/org/repo" },
    });
    fireEvent.blur(urlInput);

    expect(screen.getByTestId("url-success-message")).toBeInTheDocument();
    expect(screen.getByText(/Verified/)).toBeInTheDocument();
    expect(screen.getByTestId("url-success-message")).toHaveStyle({
      color: "green",
    });
  });
  test("accepts URL with .git extension for onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, {
      target: { value: "https://github.com/user/repo.git" },
    });
    fireEvent.blur(urlInput);

    expect(screen.queryByTestId("url-error-message")).not.toBeInTheDocument();
  });
  test("accepts URL with trailing slash for onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, {
      target: { value: "https://github.com/user/repo/" },
    });
    fireEvent.blur(urlInput);

    expect(screen.queryByTestId("url-error-message")).not.toBeInTheDocument();
  });
  test("rejects invalid characters in username for onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, {
      target: { value: "https://github.com/user@/repo" },
    });
    fireEvent.blur(urlInput);

    expect(screen.getByTestId("url-error-message")).toBeInTheDocument();
  });
  test("rejects URLs with subdomain for onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, {
      target: { value: "https://api.github.com/user/repo" },
    });
    fireEvent.blur(urlInput);

    expect(screen.getByTestId("url-error-message")).toBeInTheDocument();
  });
  test("rejects URLs with surronding text for onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, {
      target: { value: "text https://github.com/user/repo text" },
    });
    fireEvent.blur(urlInput);

    expect(screen.getByTestId("url-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(
        /Please enter a valid GitHub repository or organization URL/,
      ),
    ).toBeInTheDocument();
  });
  test("rejects URLs with text at the front for onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, {
      target: { value: "text https://github.com/user/repo" },
    });
    fireEvent.blur(urlInput);

    expect(screen.getByTestId("url-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(
        /Please enter a valid GitHub repository or organization URL/,
      ),
    ).toBeInTheDocument();
  });
  test("rejects URLs with text at the back for onBlurURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, {
      target: { value: "https://github.com/user/repo text" },
    });
    fireEvent.blur(urlInput);

    expect(screen.getByTestId("url-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(
        /Please enter a valid GitHub repository or organization URL/,
      ),
    ).toBeInTheDocument();
  });
  test("correct error message displayed when input is empty for onBlurName", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );

    const nameInput = screen.getByTestId("collection-name-input");

    fireEvent.change(nameInput, { target: { value: "" } });
    fireEvent.blur(nameInput);

    expect(screen.getByTestId("name-error-message")).toBeInTheDocument();
    expect(screen.getByText(/Collection name is required/)).toBeInTheDocument();

    const nameErrorMessage = screen.getByTestId("name-error-message");
    expect(nameErrorMessage).toHaveStyle({ color: "red" });
  });
  test("shows correct error message when name input is invalid (only spaces) on blur for onBlurName", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );

    const nameInput = screen.getByTestId("collection-name-input");

    nameInput.value = "   ";
    fireEvent.blur(nameInput);

    expect(screen.getByTestId("name-error-message")).toBeInTheDocument();
    expect(screen.getByText(/Collection name is required/)).toBeInTheDocument();
  });
  test("shows valid message when name input is valid for onBlurName", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );

    const nameInput = screen.getByTestId("collection-name-input");

    fireEvent.change(nameInput, { target: { value: "a" } });
    fireEvent.blur(nameInput);

    expect(screen.getByTestId("name-success-message")).toBeInTheDocument();
    const successMessage = screen.getByTestId("name-success-message");
    expect(successMessage.textContent).toBe("Collection name is available ");
    expect(screen.getByTestId("name-success-message")).toHaveStyle({
      color: "green",
    });
    expect(screen.getByTestId("name-success-icon")).toHaveStyle({
      color: "green",
    });
  });
  test("shows valid message when name input is valid and has spaces for onBlurName", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );

    const nameInput = screen.getByTestId("collection-name-input");

    fireEvent.change(nameInput, { target: { value: "commits " } });
    fireEvent.blur(nameInput);

    expect(screen.queryByTestId("name-error-message")).not.toBeInTheDocument();
    expect(screen.getByTestId("name-success-message")).toBeInTheDocument();
    expect(
      screen.getByText(/Collection name is available/),
    ).toBeInTheDocument();
    expect(screen.getByTestId("name-success-message")).toHaveStyle({
      color: "green",
    });
  });
  test("shows an error message when the name already exists for onBlurName", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm
            collections={collectionNames.collectionNamesForOneCourse}
          />
        </Router>
      </QueryClientProvider>,
    );

    const nameInput = screen.getByTestId("collection-name-input");

    fireEvent.change(nameInput, { target: { value: "CS156-2025-02" } });
    fireEvent.blur(nameInput);

    expect(screen.getByTestId("name-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(/Collection name already exists/),
    ).toBeInTheDocument();
    expect(screen.getByTestId("name-error-message")).toHaveStyle({
      color: "red",
    });
  });
  test("shows an error for a repeated name (with spaces) for onBlurName", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm
            collections={collectionNames.collectionNamesForOneCourse}
          />
        </Router>
      </QueryClientProvider>,
    );
    const nameInput = screen.getByTestId("collection-name-input");

    fireEvent.change(nameInput, {
      target: { value: "divy-commit-collection " },
    });

    expect(screen.getByTestId("name-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(/Collection name already exists/),
    ).toBeInTheDocument();
  });
  test("shows error message when name input is invalid (only spaces) for onBlurName", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );

    const nameInput = screen.getByTestId("collection-name-input");

    fireEvent.change(nameInput, { target: { value: " " } });
    fireEvent.blur(nameInput);

    await waitFor(() => {
      expect(screen.getByTestId("name-error-message")).toBeInTheDocument();
    });
    expect(screen.getByText(/Collection name is required/)).toBeInTheDocument();
  });
  test("doesn't show any message for URLs while typing text for onChangeURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, {
      target: { value: "https://github.co" },
    });

    expect(screen.queryByTestId("url-error-message")).not.toBeInTheDocument();
    expect(
      screen.queryByText(
        /Please enter a valid GitHub repository or organization URL/,
      ),
    ).not.toBeInTheDocument();

    expect(screen.getByTestId("url-empty-message")).toBeInTheDocument();
    const urlEmptyMessage = screen.getByTestId("url-empty-message");
    expect(urlEmptyMessage).toHaveTextContent("");
  });
  test("shows correct error for an empty string onChangeURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");

    fireEvent.change(urlInput, {
      target: { value: " " },
    });

    expect(screen.getByTestId("url-empty-message")).toBeInTheDocument();
    expect(
      screen.getByText(/GitHub repository or organization URL is required/),
    ).toBeInTheDocument();
    const urlErrorMessage = screen.getByTestId("url-empty-message");
    expect(urlErrorMessage).toHaveStyle({ color: "red" });
  });
  test("shows valid message when name input is valid for onChangeURL", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );

    const nameInput = screen.getByTestId("URL-input");

    fireEvent.change(nameInput, { target: { value: "a" } });

    expect(screen.getByTestId("url-empty-message")).toBeInTheDocument();
    expect(screen.queryByTestId("url-error-message")).not.toBeInTheDocument();
    expect(
      screen.queryByText(/GitHub repository or organization URL is required/),
    ).not.toBeInTheDocument();
  });
  test("shows an error when an empty string is passed in onChangeName", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const nameInput = screen.getByTestId("collection-name-input");

    fireEvent.change(nameInput, {
      target: { value: " " },
    });

    expect(screen.getByTestId("name-error-message")).toBeInTheDocument();
    expect(screen.getByText(/Collection name is required/)).toBeInTheDocument();

    const nameErrorMessage = screen.getByTestId("name-error-message");
    expect(nameErrorMessage).toHaveStyle({ color: "red" });
  });
  test("shows an error when a collection name that already exists is passed in onChangeName", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm
            collections={collectionNames.collectionNamesForOneCourse}
          />
        </Router>
      </QueryClientProvider>,
    );
    const nameInput = screen.getByTestId("collection-name-input");

    fireEvent.change(nameInput, {
      target: { value: "divy-commit-collection" },
    });

    expect(screen.getByTestId("name-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(/Collection name already exists/),
    ).toBeInTheDocument();
  });
  test("does not show an error when a unique collection name is passed in onChangeName", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm
            collections={collectionNames.collectionNamesForOneCourse}
          />
        </Router>
      </QueryClientProvider>,
    );
    const nameInput = screen.getByTestId("collection-name-input");

    fireEvent.change(nameInput, {
      target: { value: "divy-commit-collection-1" },
    });

    expect(screen.queryByTestId("name-error-message")).not.toBeInTheDocument();
    expect(
      screen.queryByText(/Collection name already exists/),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId("name-success-message")).toBeInTheDocument();
    expect(
      screen.getByText(/Collection name is available/),
    ).toBeInTheDocument();
    expect(screen.getByTestId("name-success-icon")).toBeInTheDocument();
    expect(screen.getByTestId("name-success-icon")).toHaveStyle({
      color: "green",
    });
  });
  test("shows an error for a repeated name (with spaces) for onChangeName", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm
            collections={collectionNames.collectionNamesForOneCourse}
          />
        </Router>
      </QueryClientProvider>,
    );
    const nameInput = screen.getByTestId("collection-name-input");

    fireEvent.change(nameInput, {
      target: { value: "divy-commit-collection " },
    });

    expect(screen.getByTestId("name-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(/Collection name already exists/),
    ).toBeInTheDocument();
  });
  test("expect placeholder for name input to be there", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const nameInput = screen.getByTestId("collection-name-input");

    expect(nameInput).toHaveStyle({ width: "300px" });
  });
  test("expect placeholder for URL input to be there", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <Router>
          <RepositorySelectionForm />
        </Router>
      </QueryClientProvider>,
    );
    const urlInput = screen.getByTestId("URL-input");
    expect(urlInput).toHaveStyle({ width: "300px" });
  });
});
