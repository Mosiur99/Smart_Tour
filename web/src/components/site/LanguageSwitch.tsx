"use client";

import { usePathname } from "next/navigation";
import { localePath, pathWithoutLocale, type Locale } from "@/i18n/config";

type LanguageSwitchProps = { locale: Locale; text: string; label: string };

/** Links to the same page in the other language, keeping the visitor's place. */
export function LanguageSwitch({ locale, text, label }: LanguageSwitchProps) {
  const pathname = usePathname();
  const other: Locale = locale === "bn" ? "en" : "bn";
  const href = localePath(other, pathWithoutLocale(pathname));

  return (
    <a
      href={href}
      hrefLang={other}
      lang={other}
      aria-label={label}
      className="flex min-h-11 items-center rounded-control px-3 font-medium text-accent hover:bg-sunk"
    >
      {text}
    </a>
  );
}
