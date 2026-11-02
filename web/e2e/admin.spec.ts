import { expect, test } from "@playwright/test";
import { expectNoAccessibilityProblems } from "./helpers";

/**
 * The API runs with a local admin user here, in place of Cloudflare Access,
 * and the website's refresh hook shares a test-only secret with it.
 */
test.describe("the admin panel", () => {
  test("a price change reaches the public page with no deploy", async ({ page }) => {
    await page.goto("/admin");
    await expect(page.getByRole("heading", { name: "Needs checking" })).toBeVisible();
    await expectNoAccessibilityProblems(page);

    await page.getByLabel("Find a destination").fill("Sylhet");
    await page.getByRole("link", { name: /^Sylhet/ }).click();
    const hotel = page.getByRole("article").filter({ has: page.getByRole("heading", { name: "Test Hotel" }) });
    await hotel.getByRole("button", { name: "Edit" }).click();
    await expectNoAccessibilityProblems(page);

    // Alternates, so a retry or a second run still makes a real change.
    const price = hotel.getByLabel("Lowest price a night (৳)");
    const next = (await price.inputValue()) === "2200" ? 2000 : 2200;
    await price.fill(String(next));
    await hotel.getByRole("button", { name: "Save changes" }).click();
    await expect(hotel.getByText("Saved.")).toBeVisible();

    await hotel.getByRole("button", { name: "History" }).click();
    await expect(hotel.getByText(/Lowest price a night/).first()).toBeVisible();

    const shown = `৳${next.toLocaleString("en-IN")} to ৳3,000`;
    await expect(async () => {
      await page.goto("/en/destinations/sylhet");
      await expect(page.getByText(shown)).toBeVisible({ timeout: 1_000 });
    }).toPass({ timeout: 20_000 });
  });

  test("a new record starts as a draft and says what it needs before publishing", async ({ page }) => {
    await page.goto("/admin/destinations/sylhet");
    await page.getByRole("button", { name: "Add a hotel" }).click();
    const name = `Draft Inn ${Date.now()}`;
    await page.getByLabel("Name in English (required)").fill(name);
    await page.getByRole("button", { name: "Add hotel as a draft" }).click();

    const hotel = page.getByRole("article").filter({ has: page.getByRole("heading", { name }) });
    await expect(hotel.getByText("Draft", { exact: true })).toBeVisible();
    await hotel.getByRole("button", { name: "Publish" }).click();
    await expect(hotel.getByRole("alert")).toHaveText(
      "Add the tier, map position, prices, source and checked date before publishing.",
    );
  });
});
