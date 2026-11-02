import { expect, test } from "@playwright/test";
import { choosePlace, expectNoAccessibilityProblems, withoutShareSheet } from "./helpers";

test.describe("making and sharing a plan", () => {
  test("in Bangla, from the form to a copied share link", async ({ page, context }) => {
    await context.grantPermissions(["clipboard-read", "clipboard-write"]);
    await withoutShareSheet(page);

    await page.goto("/");
    await expect(page.getByRole("heading", { level: 1 })).toHaveText("কোথায় যেতে চান?");
    await choosePlace(page, "কোথা থেকে", "ঢাকা", /ঢাকা/);
    await choosePlace(page, "কোথায়", "সিলে", /সিলেট/);
    await page.getByRole("button", { name: "প্ল্যান তৈরি করুন" }).click();

    await expect(page).toHaveURL(/\/plan\/[2-9a-zA-Z]{12}$/);
    await expect(page.getByRole("heading", { level: 1 })).toHaveText("৩ দিনে সিলেট");
    await expect(page.getByText("মোট আনুমানিক খরচ")).toBeVisible();
    await expect(page.getByRole("heading", { name: "দিন ১" })).toBeVisible();

    await page.getByRole("button", { name: "শেয়ার করুন" }).click();
    await expect(page.getByText("লিংক কপি হয়েছে")).toBeVisible();
    expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(page.url());
  });

  test("in English, and the shared link opens the same plan", async ({ page, browser }) => {
    await page.goto("/en");
    await choosePlace(page, "Starting from", "dha", /Dhaka/);
    await choosePlace(page, "Going to", "syl", /Sylhet/);
    await page.getByRole("button", { name: "More days" }).click();
    // People tap the visible label; the radio itself is visually hidden.
    await page.getByText("Comfort", { exact: true }).click();
    await expect(page.getByRole("radio", { name: "Comfort" })).toBeChecked();
    await page.getByRole("button", { name: "Make my plan" }).click();

    await expect(page).toHaveURL(/\/en\/plan\/[2-9a-zA-Z]{12}$/);
    await expect(page.getByRole("heading", { level: 1 })).toHaveText("4 days in Sylhet");
    const total = await page.getByText(/^৳[\d,]+ to ৳[\d,]+$/).first().textContent();

    // A friend opens the link with no shared state.
    const friend = await (await browser.newContext()).newPage();
    await friend.goto(page.url());
    await expect(friend.getByRole("heading", { level: 1 })).toHaveText("4 days in Sylhet");
    await expect(friend.getByText(total ?? "missing").first()).toBeVisible();
  });

  test("the same request returns the same link", async ({ page }) => {
    const make = async () => {
      await page.goto("/en");
      await choosePlace(page, "Starting from", "dha", /Dhaka/);
      await choosePlace(page, "Going to", "syl", /Sylhet/);
      await page.getByRole("button", { name: "Make my plan" }).click();
      await expect(page).toHaveURL(/\/plan\//);
      return page.url();
    };
    expect(await make()).toBe(await make());
  });

  test("switches the plan to the other language", async ({ page }) => {
    await page.goto("/en");
    await choosePlace(page, "Starting from", "dha", /Dhaka/);
    await choosePlace(page, "Going to", "syl", /Sylhet/);
    await page.getByRole("button", { name: "Make my plan" }).click();
    await expect(page).toHaveURL(/\/en\/plan\//);
    const id = page.url().split("/").pop();

    await page.getByRole("link", { name: "বাংলায় দেখুন" }).click();
    await expect(page).toHaveURL(new RegExp(`/plan/${id}$`));
    await expect(page.locator("html")).toHaveAttribute("lang", "bn");
  });
});

test.describe("form problems", () => {
  test("asks for both places before sending", async ({ page }) => {
    await page.goto("/en");
    await page.getByRole("button", { name: "Make my plan" }).click();
    await expect(page.getByText("Choose a starting point from the list.")).toBeVisible();
    await expect(page.getByText("Choose a destination from the list.")).toBeVisible();
    await expect(page).toHaveURL(/\/en$/);
  });

  test("explains when a destination has no checked data yet", async ({ page }) => {
    await page.goto("/en");
    await choosePlace(page, "Starting from", "dha", /Dhaka/);
    await choosePlace(page, "Going to", "bandar", /Bandarban/);
    await page.getByRole("button", { name: "Make my plan" }).click();
    await expect(page.getByRole("alert").filter({ hasText: "aren't ready yet" })).toBeVisible();
  });
});

test.describe("destination pages", () => {
  test("start a plan with the destination filled in", async ({ page }) => {
    await page.goto("/en/destinations");
    await page.getByRole("link", { name: /Sylhet/ }).first().click();
    await expect(page.getByRole("heading", { level: 1 })).toHaveText("Sylhet");

    await page.getByRole("link", { name: "Plan a trip to Sylhet" }).click();
    await expect(page.getByRole("combobox", { name: "Going to" })).toHaveValue("Sylhet");
  });

  test("link to a route guide", async ({ page }) => {
    await page.goto("/en/destinations/sylhet");
    await page.getByRole("link", { name: "Dhaka to Sylhet" }).click();
    await expect(page.getByRole("heading", { level: 1 })).toHaveText("Dhaka to Sylhet");
  });

  test("unknown addresses get the site's not-found page", async ({ page }) => {
    const response = await page.goto("/en/destinations/atlantis");
    expect(response?.status()).toBe(404);
    await expect(page.getByText("We couldn't find that page")).toBeVisible();

    const unknown = await page.goto("/no-such-page");
    expect(unknown?.status()).toBe(404);
    await expect(page.getByText("পাতাটি খুঁজে পাওয়া যায়নি")).toBeVisible();
  });
});

test.describe("accessibility in a real browser", () => {
  for (const path of ["/", "/en", "/en/destinations/sylhet", "/destinations"]) {
    test(`${path} has no problems`, async ({ page }) => {
      await page.goto(path);
      await expectNoAccessibilityProblems(page);
    });
  }

  test("a plan has no problems in the dark theme", async ({ page }) => {
    await page.emulateMedia({ colorScheme: "dark" });
    await page.goto("/en");
    await choosePlace(page, "Starting from", "dha", /Dhaka/);
    await choosePlace(page, "Going to", "syl", /Sylhet/);
    await page.getByRole("button", { name: "Make my plan" }).click();
    await expect(page).toHaveURL(/\/plan\//);
    await expectNoAccessibilityProblems(page);
  });
});

test("pages send security headers", async ({ page }) => {
  const response = await page.goto("/en");
  const headers = response?.headers() ?? {};
  expect(headers["content-security-policy"]).toContain("frame-ancestors 'none'");
  expect(headers["x-content-type-options"]).toBe("nosniff");
  expect(headers["x-powered-by"]).toBeUndefined();
});
