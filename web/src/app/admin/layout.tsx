import type { Metadata } from "next";
import { Hind_Siliguri } from "next/font/google";
import type { ReactNode } from "react";
import { AdminShell } from "@/admin/AdminShell";
import "@/styles/globals.css";

const hind = Hind_Siliguri({
  subsets: ["bengali", "latin"],
  weight: ["400", "500", "600"],
  variable: "--font-hind",
  display: "swap",
});

export const metadata: Metadata = {
  title: { default: "SmartTour admin", template: "%s · SmartTour admin" },
  robots: { index: false, follow: false },
};

/**
 * The admin panel is its own app beside the public site. Cloudflare Access
 * guards it, and the API checks every request again, so this page holds no
 * secrets. It's in English, for the team that keeps the data.
 */
export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en" className={hind.variable} suppressHydrationWarning>
      <head>
        <script src="/theme.js" />
      </head>
      <body className="flex min-h-dvh flex-col">
        <AdminShell>{children}</AdminShell>
      </body>
    </html>
  );
}
