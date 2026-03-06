import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import { useForm } from "react-hook-form";
import { SchoolTypeahead } from "main/components/Courses/SchoolTypeahead";
import { render } from "@testing-library/react";
import { Form } from "react-bootstrap";
import { fireEvent, screen, waitFor } from "@testing-library/react";
import { schoolFixtures, schoolList } from "fixtures/schoolFixtures";
import { vi } from "vitest";
import * as useBackend from "main/utils/useBackend";

const queryClient = new QueryClient();
const axiosMock = new AxiosMockAdapter(axios);

describe("SchoolDropdown tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    vi.clearAllMocks();
  });

  const RHFWrapper = ({ defaultValues = {}, mockSubmit }) => {
    {
      const { control, handleSubmit } = useForm({
        defaultValues: defaultValues,
      });

      return (
        <QueryClientProvider client={queryClient}>
          <Form onSubmit={handleSubmit(mockSubmit)}>
            <SchoolTypeahead
              control={control}
              rules={{ required: "School is required." }}
              testid="school-typeahead"
            />
            <button type="submit">Submit</button>
          </Form>
        </QueryClientProvider>
      );
    }
  };

  test("SchoolTypeahead basic typing", async () => {
    axiosMock.onGet("/api/systemInfo/schools").reply(200, schoolList);
    const mockSubmit = vi.fn();
    render(<RHFWrapper mockSubmit={mockSubmit} />);
    const typeahead = screen.getByTestId("school-typeahead");
    fireEvent.change(typeahead, { target: { value: "uc santa barbara" } });
    fireEvent.click(await screen.findByText("UCSB"));
    fireEvent.click(screen.getByText("Submit"));
    await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  });

  test("SchoolTypeahead handles value change properly", async () => {
    axiosMock.onGet("/api/systemInfo/schools").reply(200, schoolList);
    const mockSubmit = vi.fn();
    render(
      <RHFWrapper
        mockSubmit={mockSubmit}
        defaultValues={{ school: schoolFixtures.ucsb }}
      />,
    );
    const typeahead = screen.getByTestId("school-typeahead");
    fireEvent.change(typeahead, { target: { value: "chic" } });
    fireEvent.click(await screen.findByRole("option", { name: "Chico State" }));
    fireEvent.click(screen.getByText("Submit"));
    await waitFor(() => expect(mockSubmit).toHaveBeenCalled());
  });

  test("SchoolTypeahead displays no matches on no matches", async () => {
    axiosMock.onGet("/api/systemInfo/schools").reply(200, schoolList);
    const mockSubmit = vi.fn();
    render(
      <RHFWrapper
        mockSubmit={mockSubmit}
        defaultValues={{ school: schoolFixtures.ucsb }}
      />,
    );
    const typeahead = screen.getByTestId("school-typeahead");
    fireEvent.change(typeahead, { target: { value: "" } });
    await screen.findByRole("option", { name: "Chico State" });
    fireEvent.change(typeahead, { target: { value: "Cal State LA" } });
    await screen.findByText("No matches found.");
  });
  test("No misbehavior on no connection", async () => {
    axiosMock.onGet("/api/systemInfo/schools").timeout();
    const mockSubmit = vi.fn();
    render(
      <RHFWrapper
        mockSubmit={mockSubmit}
        defaultValues={{ school: schoolFixtures.ucsb }}
      />,
    );
    const typeahead = screen.getByTestId("school-typeahead");
    fireEvent.change(typeahead, { target: { value: "UC" } });
    await screen.findByText("No matches found.");
  });

  test("Various useBackend assertions", async () => {
    axiosMock.onGet("/api/systemInfo/schools").reply(200, schoolList);
    vi.spyOn(useBackend, "useBackend");
    const mockSubmit = vi.fn();
    render(
      <RHFWrapper
        mockSubmit={mockSubmit}
        defaultValues={{ school: schoolFixtures.ucsb }}
      />,
    );

    expect(useBackend.useBackend).toHaveBeenCalledWith(
      [`/api/systemInfo/schools`],
      {
        method: "GET",
        url: `/api/systemInfo/schools`,
      },
      undefined,
      true,
      {
        staleTime: "static",
      },
    );
  });
});
