import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { cache } from "react";
import { getDestination, getHubs } from "@/api/server";
import { DestinationView } from "@/components/destination/DestinationView";
import { isLocale, localName, localePath } from "@/i18n/config";
import { dictionary } from "@/i18n/dictionary";
import { routeSlug } from "@/lib/routeSlug";
import { alternates } from "@/lib/seo";

// Rendered on first visit and refreshed every ten minutes. Step 8 refreshes on publish.
export const revalidate = 600;

export function generateStaticParams() {
  return [];
}

const loadDestination = cache(getDestination);

type Props = PageProps<"/[lang]/destinations/[slug]">;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { lang, slug } = await params;
  const detail = isLocale(lang) ? await loadDestination(slug) : null;
  if (!detail || !isLocale(lang)) {
    return {};
  }
  const name = localName(lang, detail.summary);
  return {
    title: name,
    description: dictionary(lang).destination.metaDescription(name),
    alternates: alternates(lang, `/destinations/${slug}`),
  };
}

export default async function DestinationPage({ params }: Props) {
  const { lang, slug } = await params;
  if (!isLocale(lang)) {
    notFound();
  }
  const [detail, hubs] = await Promise.all([loadDestination(slug), getHubs()]);
  if (!detail) {
    notFound();
  }
  const text = dictionary(lang);
  const name = localName(lang, detail.summary);
  // One route guide per starting point, which also gives search engines a path to them.
  const routes = [...new Set(detail.transport.arriving.map((option) => option.fromHub))].flatMap((from) => {
    const hub = hubs.find((candidate) => candidate.slug === from);
    return hub
      ? [{ href: localePath(lang, `/routes/${routeSlug(from, slug)}`), label: text.route.title(localName(lang, hub), name) }]
      : [];
  });

  return (
    <>
      <script
        type="application/ld+json"
        // JSON.stringify output with < escaped cannot close the script tag.
        dangerouslySetInnerHTML={{
          __html: JSON.stringify({
            "@context": "https://schema.org",
            "@type": "TouristDestination",
            name,
            ...(detail.lat !== null && detail.lng !== null
              ? { geo: { "@type": "GeoCoordinates", latitude: detail.lat, longitude: detail.lng } }
              : {}),
          }).replace(/</g, "\\u003c"),
        }}
      />
      <DestinationView detail={detail} routes={routes} locale={lang} text={text} />
    </>
  );
}
