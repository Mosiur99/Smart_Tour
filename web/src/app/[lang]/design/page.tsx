import type { Metadata } from "next";
import { DesignPreview } from "./DesignPreview";

export const metadata: Metadata = {
  title: "Design preview",
  robots: { index: false, follow: false },
};

export default function DesignPage() {
  return <DesignPreview />;
}
