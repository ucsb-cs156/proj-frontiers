import { describe, it, expect } from "vitest";
import { JSDOM } from "jsdom";
import html from "../../../../index.html?raw";

describe("index.html", () => {
  it('should have a <title> of "Frontiers"', () => {
    const dom = new JSDOM(html);
    const { document } = dom.window;

    expect(document.title).toBe("Frontiers");
  });
});
