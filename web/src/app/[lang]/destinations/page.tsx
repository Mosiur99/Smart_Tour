import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { getAllDestinations } from "@/api/server";
import type { DestinationSummary } from "@/api/types";
import { isLocale, localName, localePath } from "@/i18n/config";
import { dictionary } from "@/i18n/dictionary";
import { alternates } from "@/lib/seo";

// Rendered per request so the build never needs the API. The API answers come
// from Next.js's data cache, refreshed every ten minutes.
export const dynamic = "force-dynamic";

type Props = PageProps<"/[lang]/destinations">;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { lang } = await params;
  if (!isLocale(lang)) {
    return {};
  }
  const text = dictionary(lang);
  return {
    title: text.destinations.title,
    description: text.destinations.intro,
    alternates: alternates(lang, "/destinations"),
  };
}

export default async function DestinationsPage({ params }: Props) {
  const { lang } = await params;
  if (!isLocale(lang)) {
    notFound();
  }
  const text = dictionary(lang);
  const destinations = await getAllDestinations();
  const divisions = groupByDivision(destinations);

  return (
    <div className="flex flex-col gap-8">
      <header className="flex flex-col gap-2">
        <h1 className="m-0 text-[32px]">{text.destinations.title}</h1>
        <p className="m-0 text-muted">{text.destinations.intro}</p>
      </header>
      {divisions.map(([division, places]) => (
        <section key={division} className="flex flex-col gap-3">
          <h2 className="m-0 text-2xl">
            {localName(lang, { nameEn: places[0].divisionNameEn, nameBn: places[0].divisionNameBn })}
          </h2>
          <ul className="m-0 grid list-none grid-cols-1 gap-2 p-0 sm:grid-cols-2">
            {places.map((place) => (
              <li key={place.slug}>
                <Link
                  href={localePath(lang, `/destinations/${place.slug}`)}
                  className="flex min-h-12 items-center justify-between gap-3 rounded-card border border-line bg-surface px-4 text-ink no-underline hover:border-stitch"
                >
                  <span className="font-semibold">{localName(lang, place)}</span>
                  {place.verified && (
                    <span className="text-sm font-semibold text-accent">{text.destinations.verified}</span>
                  )}
                </Link>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}

function groupByDivision(destinations: DestinationSummary[]): [string, DestinationSummary[]][] {
  const groups = new Map<string, DestinationSummary[]>();
  for (const place of destinations) {
    const key = place.divisionSlug ?? "";
    groups.set(key, [...(groups.get(key) ?? []), place]);
  }
  return [...groups.entries()];
}
