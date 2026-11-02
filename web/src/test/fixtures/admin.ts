import type { AdminRecord, EntitySpec, FieldSpec } from "@/api/types";
import type { AdminContextValue } from "@/admin/AdminShell";

/** Test data shaped like the admin API's answers. Used only by tests. */
function field(name: string, type: FieldSpec["type"], extra: Partial<FieldSpec> = {}): FieldSpec {
  return { name, type, options: [], min: null, max: null, required: false, createOnly: false, ...extra };
}

export const HOTEL_SPEC: EntitySpec = {
  entity: "hotels",
  fields: [
    field("nameEn", "TEXT", { required: true }),
    field("nameBn", "TEXT"),
    field("tier", "CHOICE", { options: ["BUDGET", "MID", "PREMIUM"] }),
    field("lat", "DECIMAL", { min: 20, max: 27 }),
    field("priceMin", "INTEGER", { min: 1, max: 1000000 }),
    field("priceMax", "INTEGER", { min: 1, max: 1000000 }),
    field("note", "LONG_TEXT"),
    field("sourceUrl", "URL"),
    field("checkedAt", "DATE"),
  ],
};

export const LEG_SPEC: EntitySpec = {
  entity: "legs",
  fields: [
    field("fromHub", "HUB", { required: true, createOnly: true }),
    field("toHub", "HUB", { required: true, createOnly: true }),
    field("serviceName", "TEXT", { required: true }),
    field("months", "NUMBERS", { min: 1, max: 12, required: true }),
  ],
};

export const PLACE_SPEC: EntitySpec = {
  entity: "places",
  fields: [field("nameEn", "TEXT", { required: true }), field("tags", "TAGS")],
};

export function hotelRecord(overrides: Partial<AdminRecord> = {}): AdminRecord {
  return {
    entity: "hotels",
    id: 7,
    revision: 3,
    status: "PUBLISHED",
    label: "Test Inn",
    destinationSlug: "sylhet",
    values: {
      nameEn: "Test Inn",
      nameBn: "টেস্ট ইন",
      tier: "MID",
      lat: 24.9,
      priceMin: 1500,
      priceMax: 2500,
      note: null,
      sourceUrl: "https://example.com/rates",
      checkedAt: "2026-10-01",
    },
    ...overrides,
  };
}

export function adminContext(role: "EDITOR" | "PUBLISHER" = "PUBLISHER"): AdminContextValue {
  return {
    user: { email: "owner@example.com", role },
    specs: { hotels: HOTEL_SPEC, legs: LEG_SPEC, places: PLACE_SPEC },
    hubs: [
      { slug: "dhaka", nameEn: "Dhaka", nameBn: "ঢাকা" },
      { slug: "sylhet", nameEn: "Sylhet", nameBn: "সিলেট" },
    ],
  };
}
