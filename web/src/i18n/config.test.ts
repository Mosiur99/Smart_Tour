import { describe, expect, it } from "vitest";
import { localName, localePath, pathWithoutLocale } from "./config";

describe("localePath", () => {
  it("keeps Bangla at the root", () => {
    expect(localePath("bn", "/")).toBe("/");
    expect(localePath("bn", "/plan/abc")).toBe("/plan/abc");
  });

  it("puts English under /en", () => {
    expect(localePath("en", "/")).toBe("/en");
    expect(localePath("en", "/plan/abc")).toBe("/en/plan/abc");
  });
});

describe("pathWithoutLocale", () => {
  it("reverses localePath", () => {
    for (const path of ["/", "/plan/abc", "/destinations"]) {
      expect(pathWithoutLocale(localePath("en", path))).toBe(path);
      expect(pathWithoutLocale(localePath("bn", path))).toBe(path);
    }
  });

  it("does not treat a word starting with en as the prefix", () => {
    expect(pathWithoutLocale("/entry")).toBe("/entry");
  });
});

describe("localName", () => {
  it("prefers the visitor's language", () => {
    expect(localName("bn", { nameEn: "Sylhet", nameBn: "সিলেট" })).toBe("সিলেট");
    expect(localName("en", { nameEn: "Sylhet", nameBn: "সিলেট" })).toBe("Sylhet");
  });

  it("falls back when a translation is missing", () => {
    expect(localName("bn", { nameEn: "Sylhet", nameBn: null })).toBe("Sylhet");
  });
});
