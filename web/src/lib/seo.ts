import type { Metadata } from "next";
import { localePath, type Locale } from "@/i18n/config";

/** Canonical and hreflang links, so search engines pair the two languages. */
export function alternates(locale: Locale, path: string): Metadata["alternates"] {
  return {
    canonical: localePath(locale, path),
    languages: {
      bn: localePath("bn", path),
      en: localePath("en", path),
      "x-default": localePath("bn", path),
    },
  };
}
