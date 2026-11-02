import type { FieldSpec } from "@/api/types";

/** What an input holds while someone types: text, or the ticked numbers of a list. */
export type DraftValue = string | number[];
export type Draft = Record<string, DraftValue>;

export function toDraft(fields: FieldSpec[], values: Record<string, unknown>): Draft {
  return Object.fromEntries(fields.map((field) => [field.name, draftValue(field, values[field.name])]));
}

function draftValue(field: FieldSpec, value: unknown): DraftValue {
  if (field.type === "NUMBERS") {
    return Array.isArray(value) ? value.map(Number) : [];
  }
  if (value === null || value === undefined) {
    return "";
  }
  return Array.isArray(value) ? value.join(", ") : String(value);
}

/** Turns the inputs back into API values. The server checks them again. */
export function fromDraft(fields: FieldSpec[], draft: Draft): Record<string, unknown> {
  return Object.fromEntries(fields.map((field) => [field.name, apiValue(field, draft[field.name] ?? "")]));
}

function apiValue(field: FieldSpec, value: DraftValue): unknown {
  if (Array.isArray(value)) {
    return [...value].sort((a, b) => a - b);
  }
  const text = value.trim();
  switch (field.type) {
    case "TAGS":
      return text
        .split(",")
        .map((tag) => tag.trim())
        .filter(Boolean);
    case "INTEGER":
    case "DECIMAL":
      // A number input can still hold half-typed text. It goes as text, so the server explains it.
      return text === "" ? null : Number.isFinite(Number(text)) ? Number(text) : text;
    default:
      return text === "" ? null : text;
  }
}

/** Values a new record starts with, matching the database defaults. */
export function newRecordValues(entity: string): Record<string, unknown> {
  const everyMonth = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
  switch (entity) {
    case "places":
      return { kind: "SPOT", priority: 100, tags: [] };
    case "hotels":
      return { roomCapacity: 2 };
    case "legs":
      return { mode: "BUS", months: everyMonth };
    default:
      return {};
  }
}
