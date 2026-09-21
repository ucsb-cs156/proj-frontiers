import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, test, vi } from "vitest";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

import SectionTranslator from "main/components/Sections/SectionTranslator";
import { sectionsFixtures } from "fixtures/sectionsFixtures";

const axiosMock = new AxiosMockAdapter(axios);

describe("SectionTranslator tests", () => {
  const queryClient = new QueryClient();

  beforeEach(() => {
    axiosMock.reset();
    axiosMock
      .onGet("/api/courses/7/sections")
      .reply(200, sectionsFixtures.threeSections);
  });

  test("renders the translation for the currently selected section", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <SectionTranslator courseId={7} section="0200" onChange={vi.fn()} />
      </QueryClientProvider>,
    );

    await waitFor(() =>
      expect(screen.getByTestId("SectionTranslator-translation")).toHaveValue(
        "Tue 10:00am",
      ),
    );
  });

  test("renders a blank translation when there is no matching section", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <SectionTranslator
          courseId={7}
          section="does-not-exist"
          onChange={vi.fn()}
        />
      </QueryClientProvider>,
    );

    await screen.findByTestId("SectionTranslator-select");
    expect(screen.getByTestId("SectionTranslator-translation")).toHaveValue("");
  });

  test("defaults the select to an empty value when section is undefined", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <SectionTranslator courseId={7} onChange={vi.fn()} />
      </QueryClientProvider>,
    );

    const select = await screen.findByTestId("SectionTranslator-select");
    expect(select).toHaveValue("");
  });

  test("calls onChange with the raw section value when a dropdown option is selected", async () => {
    const onChange = vi.fn();
    render(
      <QueryClientProvider client={queryClient}>
        <SectionTranslator courseId={7} section="" onChange={onChange} />
      </QueryClientProvider>,
    );

    const select = await screen.findByTestId("SectionTranslator-select");
    await screen.findByRole("option", { name: "0300 - Tue 11:00am" });

    fireEvent.change(select, { target: { value: "0300" } });

    expect(onChange).toHaveBeenCalledWith("0300");
  });

  test("lists all sections for the course as dropdown options", async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <SectionTranslator courseId={7} section="" onChange={vi.fn()} />
      </QueryClientProvider>,
    );

    const select = await screen.findByTestId("SectionTranslator-select");
    sectionsFixtures.threeSections.forEach((s) => {
      expect(
        screen.getByRole("option", { name: `${s.section} - ${s.label}` }),
      ).toBeInTheDocument();
    });
    expect(select).toBeInTheDocument();
  });
});
