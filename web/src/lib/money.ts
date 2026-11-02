export type Locale = "en" | "bn";

const NUMBER_LOCALE: Record<Locale, string> = {
  // en-IN, because en-BD groups thousands the Western way (125,000).
  en: "en-IN",
  bn: "bn-BD",
};

const RANGE_WORD: Record<Locale, string> = {
  en: "to",
  bn: "থেকে",
};

/** Formats whole taka with the South Asian grouping: ৳1,25,000. */
export function formatTaka(amount: number, locale: Locale): string {
  return `৳${new Intl.NumberFormat(NUMBER_LOCALE[locale], {
    maximumFractionDigits: 0,
  }).format(amount)}`;
}

/** Shows a single figure when both ends match, so ranges never read "৳500 to ৳500". */
export function formatTakaRange(min: number, max: number, locale: Locale): string {
  if (min === max) {
    return formatTaka(min, locale);
  }
  return `${formatTaka(min, locale)} ${RANGE_WORD[locale]} ${formatTaka(max, locale)}`;
}
