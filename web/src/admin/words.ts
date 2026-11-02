import type { AuditEntry } from "@/api/types";

/** Labels for the admin panel. It's for the team, so it's in English only. */
const FIELD_LABELS: Record<string, string> = {
  nameEn: "Name in English",
  nameBn: "Name in Bangla",
  arrivalHub: "Arrival hub",
  lat: "Latitude",
  lng: "Longitude",
  roadFactor: "Road factor",
  bestMonths: "Best months",
  minDays: "Fewest days",
  localTransportDaily: "Local transport a day (৳)",
  foodDaily: "Food a day (৳)",
  travelNote: "Travel note",
  hotelNote: "Hotel note",
  universityNote: "University note",
  guideUrl: "Guide address",
  kind: "Kind",
  priority: "Priority",
  visitMinutes: "Visit time (minutes)",
  feeMin: "Lowest fee (৳)",
  feeMax: "Highest fee (৳)",
  tags: "Tags",
  note: "Note",
  sourceUrl: "Source address",
  checkedAt: "Checked on",
  tier: "Tier",
  address: "Address",
  roomCapacity: "People a room",
  priceMin: "Lowest price a night (৳)",
  priceMax: "Highest price a night (৳)",
  website: "Website",
  fromHub: "From",
  toHub: "To",
  mode: "Travel by",
  serviceName: "Service name",
  durationMinutes: "Travel time (minutes)",
  departureNote: "Departures",
  fareMin: "Lowest fare (৳)",
  fareMax: "Highest fare (৳)",
  months: "Runs in",
  status: "Status",
};

const FIELD_HINTS: Record<string, string> = {
  roadFactor: "How much longer roads are than a straight line. Hill roads are about 1.6.",
  priority: "Lower numbers come first in plans.",
  tags: "Separate tags with commas.",
  sourceUrl: "Where the price was checked. Needed to publish a price.",
  fareMin: "Leave both fares empty when they aren't known.",
};

export const STATUS_LABELS: Record<string, string> = {
  PUBLISHED: "Published",
  IMPORTED: "Imported, not checked",
  DRAFT: "Draft",
  ARCHIVED: "Archived",
};

export const KIND_LABELS: Record<string, string> = {
  destinations: "Destination",
  places: "Place",
  hotels: "Hotel",
  legs: "Journey",
};

const ACTION_LABELS: Record<string, string> = {
  CREATE: "Added",
  UPDATE: "Edited",
  PUBLISH: "Published",
  UNPUBLISH: "Unpublished",
  ARCHIVE: "Archived",
};

export const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

export function fieldLabel(name: string): string {
  return FIELD_LABELS[name] ?? name.replace(/([A-Z])/g, " $1").replace(/^./, (first) => first.toUpperCase());
}

export function fieldHint(name: string): string | undefined {
  return FIELD_HINTS[name];
}

export function actionLabel(action: string): string {
  return ACTION_LABELS[action] ?? action;
}

const DAY = 24 * 60 * 60 * 1000;
const dateFormat = new Intl.DateTimeFormat("en-GB", { day: "numeric", month: "short", year: "numeric" });
const timeFormat = new Intl.DateTimeFormat("en-GB", { dateStyle: "medium", timeStyle: "short" });

/** "12 Oct 2026, 21 days ago" for a date like 2026-10-12. */
export function checkedLabel(isoDate: string, today: Date = new Date()): string {
  const date = new Date(`${isoDate}T00:00:00`);
  const days = Math.max(0, Math.floor((startOfDay(today) - date.getTime()) / DAY));
  const ago = days === 0 ? "today" : days === 1 ? "yesterday" : `${days} days ago`;
  return `${dateFormat.format(date)}, ${ago}`;
}

export function timeLabel(isoTime: string): string {
  return timeFormat.format(new Date(isoTime));
}

export function todayIso(today: Date = new Date()): string {
  const local = new Date(today.getTime() - today.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 10);
}

function startOfDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}

export function displayValue(value: unknown): string {
  if (value === null || value === undefined || value === "") {
    return "empty";
  }
  if (Array.isArray(value)) {
    return value.length === 0 ? "empty" : value.join(", ");
  }
  return String(value);
}

export type Change = { field: string; before: unknown; after: unknown };

/** The fields an audit entry changed, status included. */
export function changesIn(entry: AuditEntry): Change[] {
  const before = entry.before ?? {};
  const after = entry.after ?? {};
  const fields = new Set([...Object.keys(before), ...Object.keys(after)]);
  return [...fields]
    .filter((field) => !sameValue(before[field], after[field]))
    .map((field) => ({ field, before: before[field] ?? null, after: after[field] ?? null }));
}

/** Only the edited fields, so a save can't undo someone else's change to another field. */
export function editedValues(
  original: Record<string, unknown>,
  edited: Record<string, unknown>,
): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(edited).filter(([field, value]) => !sameValue(original[field], value)),
  );
}

function sameValue(a: unknown, b: unknown): boolean {
  return JSON.stringify(a ?? null) === JSON.stringify(b ?? null);
}
