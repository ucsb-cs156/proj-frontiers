import { describe, expect, test, vi, beforeEach } from "vitest";
import { toast } from "react-toastify";
import { onSectionMutationError } from "main/utils/sectionsUtils";

vi.mock("react-toastify", async (importOriginal) => {
  const mockToast = vi.fn();
  return {
    ...(await importOriginal()),
    toast: mockToast,
  };
});

describe("sectionsUtils tests", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  test("onSectionMutationError shows duplicate message on 409", () => {
    const error = new Error("Request failed with status code 409");
    error.response = { status: 409 };

    onSectionMutationError(error);

    expect(toast).toHaveBeenCalledTimes(1);
    expect(toast).toHaveBeenCalledWith(
      "A section with that section value already exists for this course.",
    );
  });

  test("onSectionMutationError shows generic message on other errors", () => {
    const error = new Error("Request failed with status code 500");
    error.response = { status: 500 };

    onSectionMutationError(error);

    expect(toast).toHaveBeenCalledTimes(1);
    expect(toast).toHaveBeenCalledWith(
      "Error: Request failed with status code 500",
    );
  });
});
