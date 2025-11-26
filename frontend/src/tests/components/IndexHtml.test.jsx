import { describe, it, expect } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

describe("index.html title", () => {
  it('has the title "Frontiers"', () => {
    const indexHtmlPath = path.resolve(__dirname, "../../../index.html");
    const html = fs.readFileSync(indexHtmlPath, "utf-8");

    expect(html).toMatch(/<title>\s*Frontiers\s*<\/title>/);
  });
});
