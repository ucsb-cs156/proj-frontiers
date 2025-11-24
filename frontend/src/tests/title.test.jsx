/**
 * @jest-environment jsdom
 */

describe("index.html title", () => {
  test("document title should be Frontiers", () => {
    document.title = "Frontiers";
    expect(document.title).toBe("Frontiers");
  });
});
