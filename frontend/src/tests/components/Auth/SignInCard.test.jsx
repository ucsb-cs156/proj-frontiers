import { FcGoogle } from "react-icons/fc";
import { render, screen } from "@testing-library/react";
import SignInCard from "main/components/Auth/SignInCard";

const exampleIcon = () => {
  return <FcGoogle size={"10em"} />;
};

describe("SignInCard Tests", () => {
  test("Card renders with required properties", () => {
    render(
      <SignInCard
        Icon={exampleIcon}
        title={"Sign In with Google"}
        description={"Boilerplate description"}
        url={"/fakeUrl/fakePath"}
        testid={"google"}
      />,
    );

    expect(screen.getByText("Sign In with Google")).toBeInTheDocument();
    expect(screen.getByText("Boilerplate description")).toBeInTheDocument();
    expect(screen.getByText("Log In")).toHaveAttribute(
      "href",
      "/fakeUrl/fakePath",
    );
    expect(screen.getByTestId("SignInCard-footer-google")).toHaveClass(
      "text-center",
    );
    expect(screen.getByTestId("SignInCard-header-google")).toHaveClass(
      "text-center",
    );
    expect(screen.getByTestId("SignInCard-base-google")).toHaveStyle(
      "width: 18rem",
    );
  });

  test("Card renders with default testId", () => {
    render(
      <SignInCard
        Icon={exampleIcon}
        title={"Sign In with Google"}
        description={"Boilerplate description"}
        url={"/fakeUrl/fakePath"}
      />,
    );

    expect(screen.getByText("Sign In with Google")).toBeInTheDocument();
    expect(screen.getByText("Boilerplate description")).toBeInTheDocument();
    expect(screen.getByText("Log In")).toHaveAttribute(
      "href",
      "/fakeUrl/fakePath",
    );
    expect(screen.getByTestId("SignInCard-footer-default")).toHaveClass(
      "text-center",
    );
    expect(screen.getByTestId("SignInCard-header-default")).toHaveClass(
      "text-center",
    );
    expect(screen.getByTestId("SignInCard-base-default")).toHaveStyle(
      "width: 18rem",
    );
  });
});
