import { render, screen, fireEvent } from "@testing-library/react";
import SingleButtonJobForm from "main/components/Jobs/SingleButtonJobForm";

describe("SingleButtonJobForm tests", () => {
  test("renders correctly with the provided text", () => {
    const text = "Test Button";
    render(<SingleButtonJobForm text={text} callback={() => {}} />);
    
    const button = screen.getByTestId("singlebutton-job-submit");
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent(text);
  });

  test("calls the callback function when clicked", () => {
    const callback = jest.fn();
    render(<SingleButtonJobForm text="Test Button" callback={callback} />);
    
    const button = screen.getByTestId("singlebutton-job-submit");
    fireEvent.click(button);
    
    expect(callback).toHaveBeenCalledTimes(1);
  });
});