import { useState } from "react";
import { Form, Button } from "react-bootstrap";
import { FaCheckCircle } from "react-icons/fa";

function RepositorySelectionForm({ collections = [] }) {
  const [url, setURL] = useState("");
  const [messageURL, setMessageURL] = useState("");

  const handleBlurURL = (e) => {
    const inputValue = e.target.value;
    setURL(inputValue);

    const inputValueTrimmed = inputValue.trim();
    if (inputValueTrimmed.length < 1) {
      setMessageURL(
        <span data-testid="url-error-message" className="text-danger">
          GitHub repository or organization URL is required
        </span>,
      );
    } else if (
      !inputValueTrimmed.match(
        /^https:\/\/github\.com\/[A-Za-z0-9_.-]+(?:\/[A-Za-z0-9_.-]+)?(?:\.git)?\/?$/,
      )
    ) {
      setMessageURL(
        <span data-testid="url-error-message" className="text-danger">
          Please enter a valid GitHub repository or organization URL
        </span>,
      );
    } else {
      setMessageURL(
        <span data-testid="url-success-message" className="text-success">
          Verified{" "}
          <FaCheckCircle
            data-testid="url-success-icon"
            className="text-success"
          />
        </span>,
      );
    }
  };
  const handleChangeURL = (e) => {
    const inputValue = e.target.value;

    const inputValueTrimmed = inputValue.trim();
    setURL(inputValue);
    if (inputValueTrimmed.length < 1) {
      setMessageURL(
        <span data-testid="url-empty-message" className="text-danger">
          GitHub repository or organization URL is required
        </span>,
      );
    } else {
      setMessageURL(<span data-testid="url-empty-message"></span>);
    }
  };

  const [name, setName] = useState("");
  const [messageName, setMessageName] = useState("");

  const handleBlurName = (e) => {
    const inputValue = e.target.value;

    const inputValueTrimmed = inputValue.trim();
    setName(inputValue);
    if (inputValueTrimmed.length < 1) {
      setMessageName(
        <span data-testid="name-error-message" className="text-danger">
          Collection name is required
        </span>,
      );
    }
  };

  const handleChangeName = (e) => {
    const inputValue = e.target.value;
    const inputValueTrimmed = inputValue.trim();

    setName(inputValue);
    if (inputValueTrimmed.length < 1) {
      setMessageName(
        <span data-testid="name-error-message" className="text-danger">
          Collection name is required
        </span>,
      );
    } else if (
      collections.some((collectionName) => collectionName === inputValueTrimmed)
    ) {
      setMessageName(
        <span data-testid="name-error-message" className="text-danger">
          Collection name already exists
        </span>,
      );
    } else {
      setMessageName(
        <span data-testid="name-success-message" className="text-success">
          Collection name is available{" "}
          <FaCheckCircle
            data-testid="name-success-icon"
            className="text-success"
          />
        </span>,
      );
    }
  };

  return (
    <Form data-testid="repository-selection-form">
      <Form.Group className="mb-3">
        <Form.Label htmlFor="collectionName">Collection Name</Form.Label>
        <div className="d-flex align-items-center gap-2">
          <Form.Control
            data-testid="collection-name-input"
            required
            type="text"
            placeholder="Ex. CS 4000 Fall 2024"
            style={{ width: "300px" }}
            value={name}
            onChange={handleChangeName}
            onBlur={handleBlurName}
          />
          <span data-testid="collection-name-message">{messageName}</span>
        </div>
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="gitRepo">
          GitHub Repository or Organization URL
        </Form.Label>
        <div className="d-flex align-items-center gap-2">
          <Form.Control
            data-testid="URL-input"
            required
            type="text"
            placeholder="Ex. https://github.com/frontiers/repo"
            style={{ width: "300px" }}
            value={url}
            onChange={handleChangeURL}
            onBlur={handleBlurURL}
          />
          <Button data-testid="add-url-button">+ Add</Button>
          <span data-testid="github-url-message">{messageURL}</span>
        </div>
      </Form.Group>
    </Form>
  );
}
export default RepositorySelectionForm;
