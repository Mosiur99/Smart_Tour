import { notFound } from "next/navigation";

/** Sends unknown addresses to the site's own not-found page instead of the framework's. */
export default function UnknownPage() {
  notFound();
}
