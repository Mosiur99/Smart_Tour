import type { Metadata } from "next";
import { Anek_Bangla, Hind_Siliguri } from "next/font/google";
import { notFound } from "next/navigation";
import type { ReactNode } from "react";
import { SiteHeader } from "@/components/site/SiteHeader";
import { isLocale, LOCALES } from "@/i18n/config";
import { dictionary } from "@/i18n/dictionary";
import "@/styles/globals.css";

const anek = Anek_Bangla({
  subsets: ["bengali", "latin"],
  weight: ["600"],
  variable: "--font-anek",
  display: "swap",
});

const hind = Hind_Siliguri({
  subsets: ["bengali", "latin"],
  weight: ["400", "500", "600"],
  variable: "--font-hind",
  display: "swap",
});

const metadataBase = new URL(process.env.SITE_URL ?? "http://localhost:3000");

export function generateStaticParams() {
  return LOCALES.map((lang) => ({ lang }));
}

export async function generateMetadata({ params }: LayoutProps<"/[lang]">): Promise<Metadata> {
  const { lang } = await params;
  const text = dictionary(isLocale(lang) ? lang : "bn");
  return {
    metadataBase,
    title: { default: text.site.name, template: `%s · ${text.site.name}` },
    description: text.site.tagline,
  };
}

export default async function LangLayout({ children, params }: LayoutProps<"/[lang]">) {
  const { lang } = await params;
  if (!isLocale(lang)) {
    notFound();
  }
  const text = dictionary(lang);

  return (
    <html lang={lang} className={`${anek.variable} ${hind.variable}`} suppressHydrationWarning>
      <head>
        {/* Blocking on purpose: it must run before paint to avoid a theme flash. */}
        <script src="/theme.js" />
      </head>
      <body className="flex min-h-dvh flex-col">
        <SiteHeader locale={lang} text={text} />
        <main id="main" className="mx-auto w-full max-w-3xl flex-1 px-5 pt-4 pb-16">
          {children as ReactNode}
        </main>
        <footer className="mx-auto w-full max-w-3xl px-5 pb-8 text-sm text-muted print:hidden">
          {text.site.footer}
        </footer>
      </body>
    </html>
  );
}
