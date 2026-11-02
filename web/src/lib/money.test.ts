import { describe, expect, it } from "vitest";
import { formatTaka, formatTakaRange } from "./money";

describe("formatTaka", () => {
  it("uses South Asian grouping in English", () => {
    expect(formatTaka(125000, "en")).toBe("৳1,25,000");
  });

  it("uses Bangla digits in Bangla", () => {
    expect(formatTaka(4200, "bn")).toBe("৳৪,২০০");
  });

  it("rounds to whole taka", () => {
    expect(formatTaka(999.6, "en")).toBe("৳1,000");
  });
});

describe("formatTakaRange", () => {
  it("joins two amounts in the right language", () => {
    expect(formatTakaRange(4200, 6800, "en")).toBe("৳4,200 to ৳6,800");
    expect(formatTakaRange(4200, 6800, "bn")).toBe("৳৪,২০০ থেকে ৳৬,৮০০");
  });

  it("shows one amount when both ends match", () => {
    expect(formatTakaRange(500, 500, "en")).toBe("৳500");
  });
});
