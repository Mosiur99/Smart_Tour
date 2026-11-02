import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { WorkspaceView } from "@/admin/WorkspaceView";

const SLUG = /^[a-z0-9]+(-[a-z0-9]+)*$/;

export async function generateMetadata({ params }: PageProps<"/admin/destinations/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  return { title: slug };
}

export default async function AdminDestination({ params }: PageProps<"/admin/destinations/[slug]">) {
  const { slug } = await params;
  if (!SLUG.test(slug)) {
    notFound();
  }
  return <WorkspaceView slug={slug} />;
}
