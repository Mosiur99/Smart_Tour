import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const css = readFileSync(join(__dirname, "tokens.css"), "utf8");

function tokensIn(block: string): Record<string, string> {
  return Object.fromEntries(
    [...block.matchAll(/--([a-z-]+):\s*(#[0-9a-f]{6});/g)].map((match) => [match[1], match[2]]),
  );
}

function luminance(hex: string): number {
  const [red, green, blue] = [1, 3, 5]
    .map((start) => parseInt(hex.slice(start, start + 2), 16) / 255)
    .map((value) => (value <= 0.03928 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4));
  return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
}

function contrast(first: string, second: string): number {
  const [lighter, darker] = [luminance(first), luminance(second)].sort((a, b) => b - a);
  return (lighter + 0.05) / (darker + 0.05);
}

const blocks = css.split(/@media|:root\[data-theme="dark"\]/);
const light = tokensIn(blocks[0]);
const mediaDark = tokensIn(blocks[1]);
const explicitDark = tokensIn(blocks[2]);

const TEXT_PAIRS = [
  ["ink", "ground"],
  ["ink", "surface"],
  ["ink", "sunk"],
  ["muted", "ground"],
  ["muted", "surface"],
  ["muted", "sunk"],
  ["accent", "ground"],
  ["accent", "surface"],
  ["on-accent", "accent"],
  ["on-accent", "accent-hover"],
  ["soft-ink", "soft"],
  ["caution", "caution-soft"],
  ["problem", "problem-soft"],
  ["problem", "ground"],
  ["problem", "surface"],
] as const;

describe.each([
  ["light", light],
  ["dark", explicitDark],
])("%s theme", (_name, tokens) => {
  it.each(TEXT_PAIRS)("%s on %s reaches AA for normal text", (text, background) => {
    expect(contrast(tokens[text], tokens[background])).toBeGreaterThanOrEqual(4.5);
  });

  it("keeps control borders visible at 3:1", () => {
    expect(contrast(tokens.stitch, tokens.surface)).toBeGreaterThanOrEqual(3);
  });
});

describe("dark palette", () => {
  it("is the same in the device setting and the explicit choice", () => {
    expect(mediaDark).toEqual(explicitDark);
  });

  it("defines every light token", () => {
    expect(Object.keys(explicitDark).sort()).toEqual(Object.keys(light).sort());
  });
});
