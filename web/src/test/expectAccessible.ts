import axe from "axe-core";
import { expect } from "vitest";

/** Colour contrast is skipped here because jsdom has no layout; tokens.test.ts covers it. */
export async function expectAccessible(container: HTMLElement) {
  const results = await axe.run(container, {
    rules: { "color-contrast": { enabled: false } },
  });
  const problems = results.violations.map(
    (violation) => `${violation.id}: ${violation.nodes.map((node) => node.html).join(", ")}`,
  );
  expect(problems).toEqual([]);
}
