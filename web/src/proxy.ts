import { NextResponse, type NextRequest } from "next/server";

/**
 * Bangla is served at the root and English under /en. Pages live under
 * app/[lang], so a Bangla address is rewritten to /bn internally, and a
 * typed /bn address redirects to the root so each page has one address.
 */
export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;

  if (pathname === "/en" || pathname.startsWith("/en/")) {
    return NextResponse.next();
  }
  if (pathname === "/bn" || pathname.startsWith("/bn/")) {
    const url = request.nextUrl.clone();
    url.pathname = pathname.slice(3) || "/";
    return NextResponse.redirect(url, 308);
  }
  const url = request.nextUrl.clone();
  url.pathname = `/bn${pathname}`;
  url.search = search;
  return NextResponse.rewrite(url);
}

export const config = {
  // Skips the API, Next.js files and anything with a file extension.
  matcher: ["/((?!api|_next|.*\\..*).*)"],
};
