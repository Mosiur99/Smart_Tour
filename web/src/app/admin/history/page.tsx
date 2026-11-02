import type { Metadata } from "next";
import { HistoryPage } from "@/admin/HistoryList";

export const metadata: Metadata = { title: "History" };

export default function AdminHistory() {
  return <HistoryPage />;
}
