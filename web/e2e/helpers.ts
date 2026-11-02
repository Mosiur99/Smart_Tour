import { join } from "node:path";
import { expect, type Page } from "@playwright/test";

const AXE = join(process.cwd(), "node_modules/axe-core/axe.min.js");

type AxeResult = { violations: { id: string; nodes: { target: string[] }[] }[] };

/** Picks a place from a search field by typing and choosing the matching option. */
export async function choosePlace(page: Page, label: string, typed: string, option: RegExp) {
  await page.getByRole("combobox", { name: label }).fill(typed);
  await page.getByRole("option", { name: option }).click();
}

/** Runs axe in a real browser, where colour contrast can be checked too. */
export async function expectNoAccessibilityProblems(page: Page) {
  await page.addScriptTag({ path: AXE });
  const violations = await page.evaluate(async () => {
    const { axe } = window as unknown as { axe: { run: () => Promise<AxeResult> } };
    const results = await axe.run();
    return results.violations.map(
      (violation) => `${violation.id}: ${violation.nodes.map((node) => node.target.join(" ")).join(", ")}`,
    );
  });
  expect(violations).toEqual([]);
}

/** Desktop Chrome has no share sheet, but a phone may: this forces the copy-link path. */
export async function withoutShareSheet(page: Page) {
  await page.addInitScript(() => {
    Object.defineProperty(Navigator.prototype, "share", { value: undefined, configurable: true });
  });
}
