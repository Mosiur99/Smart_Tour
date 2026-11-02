export const LOCALES = ["bn", "en"] as const;
export type Locale = (typeof LOCALES)[number];

/** Bangla lives at the root and English under /en, as the plan says. */
export const DEFAULT_LOCALE: Locale = "bn";

export function isLocale(value: string): value is Locale {
  return (LOCALES as readonly string[]).includes(value);
}

/** The public address of a page: "/plan/x" in Bangla, "/en/plan/x" in English. */
export function localePath(locale: Locale, path: string): string {
  const clean = path === "/" ? "" : path;
  return locale === DEFAULT_LOCALE ? clean || "/" : `/${locale}${clean}`;
}

/** Strips the language prefix from a public address, the reverse of localePath. */
export function pathWithoutLocale(pathname: string): string {
  const match = pathname.match(/^\/en(\/.*)?$/);
  return match ? (match[1] ?? "/") : pathname;
}

/** Picks the name in the visitor's language, falling back to the other one. */
export function localName(
  locale: Locale,
  names: { nameEn: string | null; nameBn: string | null },
): string {
  const preferred = locale === "bn" ? names.nameBn : names.nameEn;
  return preferred ?? names.nameEn ?? names.nameBn ?? "";
}
