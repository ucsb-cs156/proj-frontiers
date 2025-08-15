import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter as Router } from "react-router";

import RepositorySelectionForm from "main/components/RepositorySelectionForm";
import collectionNames from "fixtures/collectionNames";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const mockedNavigate = jest.fn();

jest.mock("react-router", () => ({
  ...jest.requireActual("react-router"),
  useNavigate: () => mockedNavigate,
}));

describe("RosterStudentForm tests", () => {
  const queryClient = new QueryClient();

  const expectedHeaders = [
    "Collection Name",
    "GitHub Repository or Organization URL",
  ];

  test("renders correctly with no initialContents", async () => {
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
  });
  test("correct error message displayed when input is empty", async () => {
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

    expect(screen.queryByTestId("url-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(/GitHub repository or organization URL is required/),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(
        /Please enter a valid GitHub repository or organization URL/,
      ),
    ).not.toBeInTheDocument();
  });
  test("shows correct error message when URL input is invalid (not correct format) on blur", () => {
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
      screen.queryByText(
        /Please enter a valid GitHub repository or organization URL/,
      ),
    ).toBeInTheDocument();
  });
  test("shows error message when URL input is empty on blur", () => {
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
  test("shows error message when URL input is invalid (spaces) on blur", () => {
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

    expect(screen.getByTestId("url-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(/GitHub repository or organization URL is required/),
    ).toBeInTheDocument();
  });
  test("does not show an error message when URL input is valid and has spaces", () => {
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
    expect(screen.getByText(/Verified/)).toBeInTheDocument();
  });
  test("rejects URLs missing https protocol", () => {
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
  test("rejects URL with wrong domain", () => {
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
  test("accepts URL with just org", () => {
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
  test("accepts URL with org and repo", () => {
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
  test("accepts URL with .git extension", () => {
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
  test("accepts URL with trailing slash", () => {
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
  test("rejects invalid characters in username", () => {
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
  test("rejects URLs with subdomain", () => {
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
  test("rejects URLs with surronding text", () => {
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
  test("rejects URLs with text at the front", () => {
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
  test("rejects URLs with text at the back", () => {
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

    expect(screen.queryByTestId("name-error-message")).toBeInTheDocument();
    expect(screen.getByText(/Collection name is required/)).toBeInTheDocument();
  });
  test("shows error message when name input is invalid (spaces) on blur for onBlurName", () => {
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

    expect(screen.getByTestId("name-error-message")).toBeInTheDocument();
    expect(screen.getByText(/Collection name is required/)).toBeInTheDocument();
    expect(screen.getByTestId("name-error-message")).toHaveStyle({
      color: "red",
    });
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
    expect(
      screen.getByText(/Collection name is available/),
    ).toBeInTheDocument();
    expect(screen.getByTestId("name-success-message")).toHaveStyle({
      color: "green",
    });
  });
  test("does not show an error message when name input is valid and has spaces for onBlurName", () => {
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

    expect(screen.queryByTestId("name-error-message")).toBeInTheDocument();
    expect(
      screen.getByText(/Collection name already exists/),
    ).toBeInTheDocument();
    expect(screen.getByTestId("name-error-message")).toHaveStyle({
      color: "red",
    });
  });
});
