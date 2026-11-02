const SLUG = /^[a-z0-9]+(-[a-z0-9]+)*$/;

/** Reads "dhaka-to-coxs-bazar" as the hub and destination slugs. */
export function parseRouteSlug(route: string): { from: string; to: string } | null {
  const separator = route.indexOf("-to-");
  if (separator < 0) {
    return null;
  }
  const from = route.slice(0, separator);
  const to = route.slice(separator + 4);
  return SLUG.test(from) && SLUG.test(to) ? { from, to } : null;
}

export function routeSlug(from: string, to: string): string {
  return `${from}-to-${to}`;
}
