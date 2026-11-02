import { describe, expect, it } from "vitest";
import type { Hub } from "@/api/types";
import { matchHubs, toOption } from "./placeOptions";

const HUBS: Hub[] = [
  { slug: "moulvibazar", nameEn: "Moulvibazar", nameBn: "মৌলভীবাজার" },
  { slug: "sylhet", nameEn: "Sylhet", nameBn: "সিলেট" },
  { slug: "dhaka", nameEn: "Dhaka", nameBn: "ঢাকা" },
];

describe("matchHubs", () => {
  it("matches English and Bangla names", () => {
    expect(matchHubs(HUBS, "SYL").map((hub) => hub.slug)).toEqual(["sylhet"]);
    expect(matchHubs(HUBS, "ঢাকা").map((hub) => hub.slug)).toEqual(["dhaka"]);
  });

  it("puts names that start with the query first", () => {
    expect(matchHubs(HUBS, "ba").map((hub) => hub.slug)).toEqual(["moulvibazar"]);
    expect(matchHubs([...HUBS, { slug: "barishal", nameEn: "Barishal", nameBn: "বরিশাল" }], "ba")[0].slug).toBe(
      "barishal",
    );
  });

  it("returns nothing for an empty query", () => {
    expect(matchHubs(HUBS, "  ")).toEqual([]);
  });
});

describe("toOption", () => {
  it("shows the other language beside the name", () => {
    expect(toOption(HUBS[1], "bn")).toEqual({ id: "sylhet", name: "সিলেট", otherName: "Sylhet" });
  });

  it("hides the other name when there is no translation", () => {
    expect(toOption({ slug: "x", nameEn: "Place", nameBn: null }, "bn").otherName).toBeUndefined();
  });
});
