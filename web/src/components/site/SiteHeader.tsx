import Link from "next/link";
import { ThemeToggle } from "@/components/ui/ThemeToggle";
import { localePath, type Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/dictionary";
import { LanguageSwitch } from "./LanguageSwitch";

export function SiteHeader({ locale, text }: { locale: Locale; text: Dictionary }) {
  return (
    <header className="print:hidden">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:absolute focus:m-2 focus:rounded-control focus:bg-surface focus:p-3"
      >
        {text.site.skipToContent}
      </a>
      <nav className="mx-auto flex max-w-3xl flex-wrap items-center justify-between gap-2 px-5 py-3">
        <Link
          href={localePath(locale, "/")}
          className="font-display text-xl font-semibold text-accent no-underline"
        >
          {text.site.name}
        </Link>
        <div className="flex flex-wrap items-center gap-1">
          <Link
            href={localePath(locale, "/destinations")}
            className="flex min-h-11 items-center rounded-control px-3 font-medium text-ink no-underline hover:bg-sunk"
          >
            {text.site.destinations}
          </Link>
          <LanguageSwitch
            locale={locale}
            text={text.site.switchLanguage}
            label={text.site.switchLanguageLabel}
          />
          <ThemeToggle toDarkLabel={text.site.toDark} toLightLabel={text.site.toLight} />
        </div>
      </nav>
    </header>
  );
}
