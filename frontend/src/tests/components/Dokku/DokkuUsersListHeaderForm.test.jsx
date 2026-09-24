import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import DokkuUsersListHeaderForm from "main/components/Dokku/DokkuUsersListHeaderForm";

const testId = "DokkuUsersListHeaderForm";

describe("DokkuUsersListHeaderForm tests", () => {
  test("renders an empty textarea by default", () => {
    render(<DokkuUsersListHeaderForm submitAction={vi.fn()} />);

    expect(
      screen.getByText("Extra lines at the start of dokku_users_list.csv"),
    ).toBeInTheDocument();
    const textarea = screen.getByTestId(`${testId}-dokkuUsersListHeader`);
    expect(textarea.tagName).toBe("TEXTAREA");
    expect(textarea).toHaveValue("");
    expect(textarea).toHaveAttribute("rows", "6");
    expect(textarea).toHaveAttribute(
      "placeholder",
      "username,dokku-00\nusername,dokku-01",
    );
    expect(screen.getByTestId(`${testId}-submit`)).toHaveTextContent(
      "Save Header",
    );
  });

  test("renders initialContents, with line breaks, and a custom button label", () => {
    render(
      <DokkuUsersListHeaderForm
        submitAction={vi.fn()}
        initialContents={"eci,dokku-00\neci,dokku-01"}
        buttonLabel="Update"
      />,
    );

    expect(screen.getByTestId(`${testId}-dokkuUsersListHeader`)).toHaveValue(
      "eci,dokku-00\neci,dokku-01",
    );
    expect(screen.getByTestId(`${testId}-submit`)).toHaveTextContent("Update");
  });

  test("updates the textarea when initialContents changes after the first render", () => {
    const { rerender } = render(
      <DokkuUsersListHeaderForm submitAction={vi.fn()} initialContents="" />,
    );
    expect(screen.getByTestId(`${testId}-dokkuUsersListHeader`)).toHaveValue(
      "",
    );

    rerender(
      <DokkuUsersListHeaderForm
        submitAction={vi.fn()}
        initialContents="loaded,dokku-03"
      />,
    );
    expect(screen.getByTestId(`${testId}-dokkuUsersListHeader`)).toHaveValue(
      "loaded,dokku-03",
    );
  });

  test("calls submitAction with the entered text, preserving line breaks", async () => {
    const submitAction = vi.fn();
    render(<DokkuUsersListHeaderForm submitAction={submitAction} />);

    fireEvent.change(screen.getByTestId(`${testId}-dokkuUsersListHeader`), {
      target: { value: "a,dokku-00\nb,dokku-01\n" },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction.mock.calls[0][0]).toEqual({
      dokkuUsersListHeader: "a,dokku-00\nb,dokku-01\n",
    });
  });

  test("submits an empty string to clear the header", async () => {
    const submitAction = vi.fn();
    render(
      <DokkuUsersListHeaderForm
        submitAction={submitAction}
        initialContents="old,dokku-00"
      />,
    );

    fireEvent.change(screen.getByTestId(`${testId}-dokkuUsersListHeader`), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByTestId(`${testId}-submit`));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));
    expect(submitAction.mock.calls[0][0]).toEqual({
      dokkuUsersListHeader: "",
    });
  });
});
