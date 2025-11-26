import { describe, it, expect } from "vitest";
import fs from "fs";
import path from "path";

describe("index.html", () => {
  // tests that the title is Frontiers; fails if it is not
  it("has the expected <title> Frontiers", () => {
    const indexHtmlPath = path.resolve("index.html");
    const html = fs.readFileSync(indexHtmlPath, "utf8");

    const match = html.match(/<title>\s*([^<]*)\s*<\/title>/i);
    expect(match).not.toBeNull();

    const title = match[1].trim();
    expect(title).toBe("Frontiers");
  });
});
