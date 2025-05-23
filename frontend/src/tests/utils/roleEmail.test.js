import {
  onDeleteSuccess,
  cellToAxiosParamsDelete,
} from "main/utils/RoleEmailUtils";
import mockConsole from "jest-mock-console";

const mockToast = jest.fn();
jest.mock("react-toastify", () => {
  const originalModule = jest.requireActual("react-toastify");
  return {
    __esModule: true,
    ...originalModule,
    toast: (x) => mockToast(x),
  };
});

describe("RoleEmailUtils", () => {
  describe("onDeleteSuccess", () => {
    test("It puts the message on console.log and in a toast", () => {
      // arrange
      const restoreConsole = mockConsole();

      // act
      onDeleteSuccess("abc");

      // assert
      expect(mockToast).toHaveBeenCalledWith("abc");
      expect(console.log).toHaveBeenCalled();
      const message = console.log.mock.calls[0][0];
      expect(message).toMatch("abc");

      restoreConsole();
    });
  });
  describe("cellToAxiosParamsDelete", () => {
    test("It returns the correct params", () => {
      // arrange
      const cell = { row: { values: { email: "testemail@ucsb.edu" } } };

      // act
      const result = cellToAxiosParamsDelete(cell, "admins");

      // assert
      expect(result).toEqual({
        url: "/api/admin/admins",
        method: "DELETE",
        params: { email: "testemail@ucsb.edu" },
      });
    });
  });
});
