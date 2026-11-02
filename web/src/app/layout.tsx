import type { Metadata } from "next";
import { Anek_Bangla, Hind_Siliguri } from "next/font/google";
import type { ReactNode } from "react";
import "./globals.css";

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

export const metadata: Metadata = {
  title: "SmartTour",
  description: "Plan a trip across Bangladesh with verified prices.",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en" className={`${anek.variable} ${hind.variable}`} suppressHydrationWarning>
      <head>
        {/* Blocking on purpose: it must run before paint to avoid a theme flash. */}
        {/* eslint-disable-next-line @next/next/no-sync-scripts */}
        <script src="/theme.js" />
      </head>
      <body>{children}</body>
    </html>
  );
}
