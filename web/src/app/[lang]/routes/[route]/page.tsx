import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { cache } from "react";
import { getDestination, getHubs } from "@/api/server";
import { TransportItem } from "@/components/destination/DestinationView";
import { isLocale, localName, localePath, type Locale } from "@/i18n/config";
import { dictionary } from "@/i18n/dictionary";
import { parseRouteSlug } from "@/lib/routeSlug";
import { alternates } from "@/lib/seo";

export const revalidate = 600;

export function generateStaticParams() {
  return [];
}

/** A route guide for search, such as /routes/dhaka-to-sylhet. */
const loadRoute = cache(async (route: string, locale: Locale) => {
  const parsed = parseRouteSlug(route);
  if (!parsed) {
    return null;
  }
  const [detail, hubs] = await Promise.all([getDestination(parsed.to), getHubs()]);
  const hub = hubs.find((candidate) => candidate.slug === parsed.from);
  const options = detail?.transport.arriving.filter((option) => option.fromHub === parsed.from) ?? [];
  if (!detail || !hub || options.length === 0) {
    return null;
  }
  return {
    slug: parsed.to,
    fromName: localName(locale, hub),
    toName: localName(locale, detail.summary),
    options,
  };
});

type Props = PageProps<"/[lang]/routes/[route]">;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { lang, route } = await params;
  const guide = isLocale(lang) ? await loadRoute(route, lang) : null;
  if (!guide || !isLocale(lang)) {
    return {};
  }
  const text = dictionary(lang);
  return {
    title: text.route.title(guide.fromName, guide.toName),
    description: text.route.metaDescription(guide.fromName, guide.toName),
    alternates: alternates(lang, `/routes/${route}`),
  };
}

export default async function RoutePage({ params }: Props) {
  const { lang, route } = await params;
  if (!isLocale(lang)) {
    notFound();
  }
  const guide = await loadRoute(route, lang);
  if (!guide) {
    notFound();
  }
  const text = dictionary(lang);

  return (
    <article className="flex flex-col gap-6">
      <header className="flex flex-col gap-2">
        <h1 className="m-0 text-[32px]">{text.route.title(guide.fromName, guide.toName)}</h1>
        <p className="m-0 text-muted">{text.route.intro(guide.fromName, guide.toName)}</p>
      </header>
      <ul className="m-0 flex list-none flex-col gap-3 p-0">
        {guide.options.map((option, index) => (
          <TransportItem key={`${option.serviceName}-${index}`} option={option} locale={lang} text={text} />
        ))}
      </ul>
      <Link
        href={`${localePath(lang, "/")}?to=${guide.slug}`}
        className="flex min-h-12 items-center justify-center self-start rounded-control bg-accent px-5 font-semibold text-on-accent no-underline hover:bg-accent-hover"
      >
        {text.destination.planHere(guide.toName)}
      </Link>
    </article>
  );
}
