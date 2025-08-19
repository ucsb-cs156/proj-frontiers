import axios from "axios";
import { fireEvent, render, waitFor, screen } from "@testing-library/react";
import AssignmentTabComponent from "main/components/TabComponent/AssignmentTabComponent";
import AxiosMockAdapter from "axios-mock-adapter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi } from "vitest";

const axiosMock = new AxiosMockAdapter(axios);
const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});
test("Calls successfully", async () => {
  axiosMock.onPost("/api/repos/createRepos").reply(200);
  const client = new QueryClient();
  render(
    <QueryClientProvider client={client}>
      <AssignmentTabComponent courseId={7} />
    </QueryClientProvider>,
  );

  await screen.findByTestId("IndividualAssignmentForm-submit");
  fireEvent.change(screen.getByLabelText("Repository Prefix"), {
    target: { value: "test" },
  });
  fireEvent.click(screen.getByTestId("IndividualAssignmentForm-submit"));
  await waitFor(() => expect(mockToast).toHaveBeenCalled());
  expect(mockToast).toBeCalledWith("Repository creation successfully started.");
  expect(axiosMock.history.post.length).toEqual(1);
  expect(axiosMock.history.post[0].params).toEqual({
    courseId: 7,
    repoPrefix: "test",
    isPrivate: false,
    permissions: "MAINTAIN",
  });
});
