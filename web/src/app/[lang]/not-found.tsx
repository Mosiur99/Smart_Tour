import Link from "next/link";

/**
 * Next.js gives not-found pages no route parameters, so this page says it
 * in both languages rather than guessing which one the visitor reads.
 */
export default function NotFound() {
  return (
    <div className="flex flex-col gap-6 py-8">
      <section lang="bn" className="flex flex-col gap-2">
        <h1 className="m-0 text-[32px]">পাতাটি খুঁজে পাওয়া যায়নি</h1>
        <p className="m-0 text-muted">লিংকটি ভুল হতে পারে, বা পাতাটি সরানো হয়েছে।</p>
        <Link href="/" className="text-accent">
          প্রথম পাতায় যান
        </Link>
      </section>
      <section lang="en" className="flex flex-col gap-2">
        <h2 className="m-0 text-2xl">We couldn&apos;t find that page</h2>
        <p className="m-0 text-muted">The link may be wrong, or the page may have moved.</p>
        <Link href="/en" className="text-accent">
          Go to the home page
        </Link>
      </section>
    </div>
  );
}
