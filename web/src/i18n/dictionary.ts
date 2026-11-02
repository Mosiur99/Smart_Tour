import { bn } from "./bn";
import type { Locale } from "./config";
import { en, type Dictionary } from "./en";

const DICTIONARIES: Record<Locale, Dictionary> = { bn, en };

export function dictionary(locale: Locale): Dictionary {
  return DICTIONARIES[locale];
}

export type { Dictionary };
