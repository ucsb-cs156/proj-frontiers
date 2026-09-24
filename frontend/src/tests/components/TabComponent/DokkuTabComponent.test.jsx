import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { toast } from "react-toastify";

import DokkuTabComponent from "main/components/TabComponent/DokkuTabComponent";
import { dokkuAccountTranslationsFixtures } from "fixtures/dokkuAccountTranslationsFixtures";

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

const axiosMock = new AxiosMockAdapter(axios);
const queryClient = new QueryClient();
const testId = "InstructorCourseShowPage";
const tableId = `${testId}-dokku-translations-table`;
const formId = "DokkuAccountTranslationsForm";

const translationsUrl = "/api/dokku/translations";
const translationsGetHistory = () =>
  axiosMock.history.get.filter((request) => request.url === translationsUrl);
const headerUrl = "/api/dokku/users_list_header";
const headerGetHistory = () =>
  axiosMock.history.get.filter((request) => request.url === headerUrl);
const headerFormId = "DokkuUsersListHeaderForm";

const renderTab = () =>
  render(
    <QueryClientProvider client={queryClient}>
      <DokkuTabComponent courseId={7} testIdPrefix={testId} />
    </QueryClientProvider>,
  );

describe("DokkuTabComponent tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    queryClient.clear();
    vi.resetAllMocks();
    axiosMock.onGet(headerUrl).reply(200, "");
  });

  test("renders the download card, the translations card and the table from the backend", async () => {
    axiosMock
      .onGet(translationsUrl)
      .reply(200, dokkuAccountTranslationsFixtures.threeTranslations);

    renderTab();

    expect(
      screen.getByTestId(`${testId}-dokku-tab-component`),
    ).toBeInTheDocument();
    expect(screen.getByText("Dokku Users List")).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-dokku-download-button`),
    ).toHaveTextContent("Download dokku_users_list.csv");
    expect(
      screen.getByText("Dokku Account Translations", {
        selector: ".card-header",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-dokku-download-text`),
    ).toHaveTextContent(
      "Download dokku_users_list.csv, the file used to set up access to the dokku servers. " +
        "It has one username,dokku-nn line per grant of access: every staff member of this " +
        "course gets access to every dokku instance, and each member of a team whose name " +
        "ends in -nn gets access to dokku-nn. The username is the part of the email before " +
        "the @, unless the email has a translation in the table below.",
    );
    expect(
      screen.getByTestId(`${testId}-dokku-translations-text`),
    ).toHaveTextContent(
      "Use this table for the case when a person's dokku username does not match the part " +
        "of their email that comes before the @ sign. Each row maps an email address to the " +
        "dokku username to use for it instead. The table is shared by all courses.",
    );
    expect(
      screen.getByTestId(`${testId}-create-dokku-translation-button`),
    ).toHaveTextContent("Create Translation");
    expect(screen.getByTestId(tableId)).toBeInTheDocument();

    expect(
      await screen.findByTestId(`${tableId}-cell-row-0-col-email`),
    ).toHaveTextContent("cgaucho@ucsb.edu");
    expect(
      screen.getByTestId(`${tableId}-cell-row-2-col-username`),
    ).toHaveTextContent("pconrad");
    expect(
      screen.getByTestId(`${tableId}-cell-row-0-col-Edit-button`),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${tableId}-cell-row-0-col-Delete-button`),
    ).toBeInTheDocument();

    expect(translationsGetHistory().length).toBe(1);
    expect(translationsGetHistory()[0].params).toEqual({ courseId: 7 });
    expect(
      screen.queryByText("Create Dokku Account Translation", {
        selector: ".modal-title",
      }),
    ).not.toBeInTheDocument();
  });

  test("download button opens the dokku_users_list.csv URL in a new tab", async () => {
    axiosMock.onGet(translationsUrl).reply(200, []);
    const download = vi.fn();
    window.open = (a, b) => download(a, b);

    renderTab();

    fireEvent.click(screen.getByTestId(`${testId}-dokku-download-button`));

    await waitFor(() => expect(download).toBeCalled());
    expect(download).toBeCalledWith(
      "/api/dokku/dokku_users_list?courseId=7",
      "_blank",
    );
    expect(download).toHaveBeenCalledTimes(1);
  });

  test("create button opens modal, submitting sends POST and closes modal", async () => {
    axiosMock.onGet(translationsUrl).reply(200, []);
    axiosMock
      .onPost(translationsUrl)
      .reply(200, { email: "new@ucsb.edu", username: "newname" });

    renderTab();

    fireEvent.click(
      screen.getByTestId(`${testId}-create-dokku-translation-button`),
    );

    expect(
      await screen.findByText("Create Dokku Account Translation", {
        selector: ".modal-title",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId(`${testId}-create-dokku-translation-modal`),
    ).toHaveClass("modal-dialog modal-dialog-centered");
    expect(screen.getByTestId(`${formId}-email`)).toHaveValue("");
    expect(screen.getByTestId(`${formId}-email`)).not.toBeDisabled();
    expect(screen.getByTestId(`${formId}-submit`)).toHaveTextContent("Create");

    fireEvent.change(screen.getByTestId(`${formId}-email`), {
      target: { value: "new@ucsb.edu" },
    });
    fireEvent.change(screen.getByTestId(`${formId}-username`), {
      target: { value: "newname" },
    });
    fireEvent.click(screen.getByTestId(`${formId}-submit`));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    expect(axiosMock.history.post[0].url).toBe(translationsUrl);
    expect(axiosMock.history.post[0].params).toEqual({
      courseId: 7,
      email: "new@ucsb.edu",
      username: "newname",
    });

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "Dokku account translation successfully created.",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    await waitFor(() =>
      expect(
        screen.queryByText("Create Dokku Account Translation", {
          selector: ".modal-title",
        }),
      ).not.toBeInTheDocument(),
    );
    // the translations list is refetched after a successful create
    await waitFor(() => expect(translationsGetHistory().length).toBe(2));
    expect(translationsGetHistory()[1].params).toEqual({ courseId: 7 });
  });

  test("create modal can be closed without submitting", async () => {
    axiosMock.onGet(translationsUrl).reply(200, []);

    renderTab();

    fireEvent.click(
      screen.getByTestId(`${testId}-create-dokku-translation-button`),
    );
    expect(
      await screen.findByText("Create Dokku Account Translation", {
        selector: ".modal-title",
      }),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText("Close"));

    await waitFor(() =>
      expect(
        screen.queryByText("Create Dokku Account Translation", {
          selector: ".modal-title",
        }),
      ).not.toBeInTheDocument(),
    );
    expect(axiosMock.history.post.length).toBe(0);
    expect(toast).not.toHaveBeenCalled();
  });

  test("create shows duplicate message on 409 and keeps modal open", async () => {
    axiosMock
      .onGet(translationsUrl)
      .reply(200, dokkuAccountTranslationsFixtures.oneTranslation);
    axiosMock.onPost(translationsUrl).reply(409);

    renderTab();

    fireEvent.click(
      screen.getByTestId(`${testId}-create-dokku-translation-button`),
    );
    expect(
      await screen.findByText("Create Dokku Account Translation", {
        selector: ".modal-title",
      }),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByTestId(`${formId}-email`), {
      target: { value: "cgaucho@ucsb.edu" },
    });
    fireEvent.change(screen.getByTestId(`${formId}-username`), {
      target: { value: "duplicate" },
    });
    fireEvent.click(screen.getByTestId(`${formId}-submit`));

    await waitFor(() => expect(axiosMock.history.post.length).toBe(1));
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "A dokku account translation for that email already exists.",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    expect(
      screen.getByText("Create Dokku Account Translation", {
        selector: ".modal-title",
      }),
    ).toBeInTheDocument();
  });

  test("editing a translation through the table refetches the list", async () => {
    axiosMock
      .onGet(translationsUrl)
      .reply(200, dokkuAccountTranslationsFixtures.threeTranslations);
    axiosMock
      .onPut(translationsUrl)
      .reply(200, { email: "cgaucho@ucsb.edu", username: "cg2" });

    renderTab();

    fireEvent.click(
      await screen.findByTestId(`${tableId}-cell-row-0-col-Edit-button`),
    );
    expect(
      await screen.findByTestId(`${tableId}-edit-modal-body`),
    ).toBeInTheDocument();
    fireEvent.change(screen.getByTestId(`${formId}-username`), {
      target: { value: "cg2" },
    });
    fireEvent.click(screen.getByTestId(`${formId}-submit`));

    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    expect(axiosMock.history.put[0].params).toEqual({
      courseId: 7,
      email: "cgaucho@ucsb.edu",
      username: "cg2",
    });
    await waitFor(() => expect(translationsGetHistory().length).toBe(2));
  });

  test("deleting a translation through the table refetches the list", async () => {
    axiosMock
      .onGet(translationsUrl)
      .reply(200, dokkuAccountTranslationsFixtures.threeTranslations);
    axiosMock.onDelete(translationsUrl).reply(200, {
      message: "Dokku account translation for ldelplaya@ucsb.edu deleted",
    });

    renderTab();

    fireEvent.click(
      await screen.findByTestId(`${tableId}-cell-row-1-col-Delete-button`),
    );
    expect(
      await screen.findByTestId("ConfirmationModal-base"),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByText("Yes, I'd like to do this"));

    await waitFor(() => expect(axiosMock.history.delete.length).toBe(1));
    expect(axiosMock.history.delete[0].params).toEqual({
      courseId: 7,
      email: "ldelplaya@ucsb.edu",
    });
    await waitFor(() => expect(translationsGetHistory().length).toBe(2));
  });

  test("renders the header card with the header loaded from the backend", async () => {
    axiosMock.onGet(translationsUrl).reply(200, []);
    axiosMock.onGet(headerUrl).reply(200, "eci,dokku-00\neci,dokku-01");

    renderTab();

    expect(screen.getByText("Dokku Users List Header")).toBeInTheDocument();
    expect(screen.getByTestId(`${testId}-dokku-header-text`)).toHaveTextContent(
      "Lines entered here are placed, exactly as written, at the very start of " +
        "dokku_users_list.csv, before the lines generated for staff and teams. Use it " +
        "for people who need dokku access but are not part of this course, such as ECI " +
        "staff or people working on projects outside the course enrollment. One " +
        "username,dokku-nn entry per line.",
    );
    expect(
      screen.getByTestId(`${headerFormId}-dokkuUsersListHeader`),
    ).toHaveValue("");

    await waitFor(() =>
      expect(
        screen.getByTestId(`${headerFormId}-dokkuUsersListHeader`),
      ).toHaveValue("eci,dokku-00\neci,dokku-01"),
    );
    expect(headerGetHistory().length).toBe(1);
    expect(headerGetHistory()[0].params).toEqual({ courseId: 7 });
  });

  test("saving the header sends it as a text/plain PUT body, toasts and refetches", async () => {
    axiosMock.onGet(translationsUrl).reply(200, []);
    axiosMock.onGet(headerUrl).reply(200, "old,dokku-00");
    axiosMock.onPut(headerUrl).reply(200, "new,dokku-00\nnew,dokku-01");

    renderTab();

    await waitFor(() =>
      expect(
        screen.getByTestId(`${headerFormId}-dokkuUsersListHeader`),
      ).toHaveValue("old,dokku-00"),
    );

    fireEvent.change(
      screen.getByTestId(`${headerFormId}-dokkuUsersListHeader`),
      { target: { value: "new,dokku-00\nnew,dokku-01" } },
    );
    fireEvent.click(screen.getByTestId(`${headerFormId}-submit`));

    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    expect(axiosMock.history.put[0].url).toBe(headerUrl);
    expect(axiosMock.history.put[0].params).toEqual({ courseId: 7 });
    expect(axiosMock.history.put[0].data).toBe("new,dokku-00\nnew,dokku-01");
    expect(axiosMock.history.put[0].headers["Content-Type"]).toBe("text/plain");

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith("Dokku users list header saved."),
    );
    expect(toast).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(headerGetHistory().length).toBe(2));
    expect(translationsGetHistory().length).toBe(1);
  });

  test("a failed header save toasts the error", async () => {
    axiosMock.onGet(translationsUrl).reply(200, []);
    axiosMock.onPut(headerUrl).reply(500);

    renderTab();

    fireEvent.change(
      screen.getByTestId(`${headerFormId}-dokkuUsersListHeader`),
      { target: { value: "x,dokku-00" } },
    );
    fireEvent.click(screen.getByTestId(`${headerFormId}-submit`));

    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        "Error: Request failed with status code 500",
      ),
    );
    expect(toast).toHaveBeenCalledTimes(1);
  });

  test("does not toast when the header request fails, and leaves the textarea empty", async () => {
    axiosMock.onGet(translationsUrl).reply(200, []);
    axiosMock.onGet(headerUrl).reply(500);

    renderTab();

    await waitFor(() => expect(headerGetHistory().length).toBe(1));
    expect(toast).not.toHaveBeenCalled();
    expect(
      screen.getByTestId(`${headerFormId}-dokkuUsersListHeader`),
    ).toHaveValue("");
  });

  test("does not toast when the translations request fails", async () => {
    axiosMock.onGet(translationsUrl).reply(500);

    renderTab();

    await waitFor(() => expect(translationsGetHistory().length).toBe(1));
    expect(toast).not.toHaveBeenCalled();
    expect(screen.getByTestId(tableId)).toBeInTheDocument();
  });
});
