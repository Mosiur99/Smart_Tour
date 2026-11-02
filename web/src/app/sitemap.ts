import type { MetadataRoute } from "next";
import { getAllDestinations } from "@/api/server";
import { localePath, LOCALES } from "@/i18n/config";

export const dynamic = "force-dynamic";

const SITE_URL = process.env.SITE_URL ?? "http://localhost:3000";

/** Every public page in both languages. Plans are personal, so they are left out. */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const destinations = await getAllDestinations();
  const paths = ["/", "/destinations", ...destinations.map((place) => `/destinations/${place.slug}`)];

  return paths.map((path) => ({
    url: `${SITE_URL}${localePath("bn", path)}`,
    alternates: {
      languages: Object.fromEntries(LOCALES.map((locale) => [locale, `${SITE_URL}${localePath(locale, path)}`])),
    },
  }));
}
