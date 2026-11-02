import { describe, expect, it } from "vitest";
import { dictionary } from "./dictionary";
import { formatCount, formatDuration, formatMonthYear, formatNumber, monthNames } from "./format";

describe("formatDuration", () => {
  it("writes hours and minutes in English", () => {
    expect(formatDuration(270, "en", dictionary("en"))).toBe("4 h 30 m");
    expect(formatDuration(120, "en", dictionary("en"))).toBe("2 h");
    expect(formatDuration(45, "en", dictionary("en"))).toBe("45 min");
  });

  it("writes Bangla digits and words in Bangla", () => {
    expect(formatDuration(270, "bn", dictionary("bn"))).toBe("৪ ঘণ্টা ৩০ মিনিট");
  });
});

describe("formatCount", () => {
  it("uses the singular for one in English", () => {
    expect(formatCount(dictionary("en").count.days, 1, "en")).toBe("1 day");
    expect(formatCount(dictionary("en").count.days, 3, "en")).toBe("3 days");
  });

  it("writes Bangla digits", () => {
    expect(formatCount(dictionary("bn").count.people, 2, "bn")).toBe("২ জন");
  });
});

describe("formatNumber", () => {
  it("uses the visitor's digits", () => {
    expect(formatNumber(3, "bn")).toBe("৩");
    expect(formatNumber(3, "en")).toBe("3");
  });
});

describe("dates", () => {
  it("names the month a price was checked", () => {
    expect(formatMonthYear("2026-10-14", "en")).toBe("Oct 2026");
  });

  it("lists twelve months in order", () => {
    expect(monthNames("en")[0]).toBe("January");
    expect(monthNames("en")[11]).toBe("December");
    expect(monthNames("bn")).toHaveLength(12);
  });
});
