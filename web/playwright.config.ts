import { defineConfig, devices } from "@playwright/test";

/**
 * End-to-end journeys on a phone-sized screen. They need the API running
 * with e2e/test-data.sql loaded; see the README. Locally they use the
 * installed Chrome (PW_CHANNEL=chrome) so no browser download is needed.
 */
const PORT = 3917;

export default defineConfig({
  testDir: "e2e",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  // The first visit to each page renders it on a cold server, which can pass 5 seconds.
  expect: { timeout: 10_000 },
  reporter: process.env.CI ? [["github"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL: `http://localhost:${PORT}`,
    trace: "retain-on-failure",
  },
  projects: [
    {
      name: "phone",
      use: { ...devices["Pixel 7"], channel: process.env.PW_CHANNEL },
    },
  ],
  webServer: {
    command: `npx next start -p ${PORT}`,
    url: `http://localhost:${PORT}/en`,
    reuseExistingServer: !process.env.CI,
  },
});
