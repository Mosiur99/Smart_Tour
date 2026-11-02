import { describe, expect, it } from "vitest";
import { changesIn, checkedLabel, displayValue, editedValues, fieldLabel, todayIso } from "./words";

describe("editedValues", () => {
  it("keeps only the fields that changed", () => {
    const original = { priceMin: 1500, priceMax: 2500, tags: ["tea"], note: null };
    const edited = { priceMin: 1800, priceMax: 2500, tags: ["tea"], note: null };

    expect(editedValues(original, edited)).toEqual({ priceMin: 1800 });
  });

  it("treats a missing value and an empty one as the same", () => {
    expect(editedValues({ note: null }, { note: undefined })).toEqual({});
  });
});

describe("changesIn", () => {
  it("lists each field whose value changed, status included", () => {
    const entry = {
      id: 1,
      actor: "owner@example.com",
      entity: "hotels",
      entityId: 7,
      action: "PUBLISH",
      before: { status: "DRAFT", priceMin: 1500, tags: ["a"] },
      after: { status: "PUBLISHED", priceMin: 1500, tags: ["a"] },
      at: "2026-11-02T10:00:00Z",
    };

    expect(changesIn(entry)).toEqual([{ field: "status", before: "DRAFT", after: "PUBLISHED" }]);
  });
});

describe("labels", () => {
  it("names known fields and spells out unknown ones", () => {
    expect(fieldLabel("priceMin")).toBe("Lowest price a night (৳)");
    expect(fieldLabel("closedDays")).toBe("Closed Days");
  });

  it("says how long ago something was checked", () => {
    const today = new Date(2026, 10, 2);
    expect(checkedLabel("2026-11-02", today)).toBe("2 Nov 2026, today");
    expect(checkedLabel("2026-11-01", today)).toBe("1 Nov 2026, yesterday");
    expect(checkedLabel("2026-07-01", today)).toBe("1 Jul 2026, 124 days ago");
  });

  it("writes today's date in the local time zone", () => {
    expect(todayIso(new Date(2026, 0, 5, 23, 30))).toBe("2026-01-05");
  });

  it("shows empty values as a word", () => {
    expect(displayValue(null)).toBe("empty");
    expect(displayValue([])).toBe("empty");
    expect(displayValue([1, 2])).toBe("1, 2");
  });
});
