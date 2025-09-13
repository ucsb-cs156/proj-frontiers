import { cellToAxiosParamsDelete } from "main/utils/rosterStudentUtils";
import { vi } from "vitest";

const mockToast = vi.fn();
vi.mock("react-toastify", async (importOriginal) => {
  return {
    ...(await importOriginal()),
    toast: (x) => mockToast(x),
  };
});

describe("rosterStudentUtils", () => {
  describe("cellToAxiosParamsDelete", () => {
    test("It returns the correct params", () => {
      // arrange
      const formReturn = {
        id: 2,
        removeFromOrg: false,
      };

      // act
      const result = cellToAxiosParamsDelete(formReturn);

      // assert
      expect(result).toEqual({
        url: "/api/rosterstudents/delete",
        method: "DELETE",
        params: { id: 2, removeFromOrg: false },
      });
    });
  });
});
