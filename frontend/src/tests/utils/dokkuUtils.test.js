import { describe, expect, test, vi } from "vitest";
import { toast } from "react-toastify";
import {
  dokkuTranslationsQueryKey,
  dokkuUsersListHeaderQueryKey,
  dokkuUsersListUrl,
  onDokkuTranslationMutationError,
} from "main/utils/dokkuUtils";

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

describe("dokkuUtils tests", () => {
  test("dokkuTranslationsQueryKey includes the course id", () => {
    expect(dokkuTranslationsQueryKey(7)).toBe(
      "/api/dokku/translations?courseId=7",
    );
    expect(dokkuTranslationsQueryKey("12")).toBe(
      "/api/dokku/translations?courseId=12",
    );
  });

  test("dokkuUsersListHeaderQueryKey includes the course id", () => {
    expect(dokkuUsersListHeaderQueryKey(7)).toBe(
      "/api/dokku/users_list_header?courseId=7",
    );
    expect(dokkuUsersListHeaderQueryKey("12")).toBe(
      "/api/dokku/users_list_header?courseId=12",
    );
  });

  test("dokkuUsersListUrl includes the course id", () => {
    expect(dokkuUsersListUrl(7)).toBe("/api/dokku/dokku_users_list?courseId=7");
    expect(dokkuUsersListUrl("12")).toBe(
      "/api/dokku/dokku_users_list?courseId=12",
    );
  });

  test("onDokkuTranslationMutationError toasts the duplicate message on 409", () => {
    onDokkuTranslationMutationError({ response: { status: 409 } });
    expect(toast).toHaveBeenCalledWith(
      "A dokku account translation for that email already exists.",
    );
  });

  test("onDokkuTranslationMutationError toasts the error otherwise", () => {
    const error = new Error("Request failed with status code 500");
    error.response = { status: 500 };
    onDokkuTranslationMutationError(error);
    expect(toast).toHaveBeenCalledWith(
      "Error: Request failed with status code 500",
    );
  });
});
