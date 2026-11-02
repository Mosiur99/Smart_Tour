import { describe, expect, it } from "vitest";
import { HOTEL_SPEC, LEG_SPEC, PLACE_SPEC } from "@/test/fixtures/admin";
import { fromDraft, newRecordValues, toDraft } from "./formValues";

describe("toDraft and fromDraft", () => {
  it("round-trips a record without changing it", () => {
    const values = {
      nameEn: "Test Inn",
      nameBn: null,
      tier: "MID",
      lat: 24.9,
      priceMin: 1500,
      priceMax: 2500,
      note: null,
      sourceUrl: "https://example.com",
      checkedAt: "2026-10-01",
    };

    expect(fromDraft(HOTEL_SPEC.fields, toDraft(HOTEL_SPEC.fields, values))).toEqual(values);
  });

  it("turns blank inputs into empty values and trims text", () => {
    const draft = { ...toDraft(HOTEL_SPEC.fields, {}), nameEn: "  Lake View  ", priceMin: " " };

    expect(fromDraft(HOTEL_SPEC.fields, draft)).toMatchObject({ nameEn: "Lake View", priceMin: null, tier: null });
  });

  it("keeps half-typed numbers as text, so the server can explain them", () => {
    const draft = { ...toDraft(HOTEL_SPEC.fields, {}), priceMin: "12e" };

    expect(fromDraft(HOTEL_SPEC.fields, draft).priceMin).toBe("12e");
  });

  it("sorts ticked months", () => {
    const draft = { ...toDraft(LEG_SPEC.fields, {}), months: [12, 1, 6] };

    expect(fromDraft(LEG_SPEC.fields, draft).months).toEqual([1, 6, 12]);
  });

  it("splits tags on commas and drops empty ones", () => {
    const draft = toDraft(PLACE_SPEC.fields, { tags: ["tea", "hills"] });
    expect(draft.tags).toBe("tea, hills");

    expect(fromDraft(PLACE_SPEC.fields, { ...draft, tags: "tea, , lakes ," }).tags).toEqual(["tea", "lakes"]);
  });
});

describe("newRecordValues", () => {
  it("starts journeys in every month, like the database", () => {
    expect(newRecordValues("legs").months).toHaveLength(12);
    expect(newRecordValues("hotels")).toEqual({ roomCapacity: 2 });
  });
});
