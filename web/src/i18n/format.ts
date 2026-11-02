import type { Locale } from "./config";
import type { Dictionary } from "./en";

const NUMBER_LOCALE: Record<Locale, string> = { bn: "bn-BD", en: "en-IN" };

/** Digits in the visitor's script: ৩ in Bangla, 3 in English. */
export function formatNumber(value: number, locale: Locale): string {
  return new Intl.NumberFormat(NUMBER_LOCALE[locale]).format(value);
}

export function formatDuration(totalMinutes: number, locale: Locale, text: Dictionary): string {
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours === 0) {
    return text.time.minutes(formatNumber(minutes, locale));
  }
  if (minutes === 0) {
    return text.time.hoursOnly(formatNumber(hours, locale));
  }
  return text.time.hours(formatNumber(hours, locale), formatNumber(minutes, locale));
}

/** "Oct 2026" or "অক্টোবর ২০২৬", for "checked" labels. */
export function formatMonthYear(isoDate: string, locale: Locale): string {
  return new Intl.DateTimeFormat(NUMBER_LOCALE[locale], { month: "short", year: "numeric" }).format(
    new Date(`${isoDate}T00:00:00Z`),
  );
}

export function monthNames(locale: Locale): string[] {
  const format = new Intl.DateTimeFormat(NUMBER_LOCALE[locale], { month: "long", timeZone: "UTC" });
  return Array.from({ length: 12 }, (_, index) => format.format(new Date(Date.UTC(2026, index, 1))));
}
