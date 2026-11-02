import { redirect } from "next/navigation";

// The home page arrives in Step 7. Until then the root shows the design preview.
export default function Home() {
  redirect("/design");
}
