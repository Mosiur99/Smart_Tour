"use client";

import { useEffect, useState } from "react";
import { Button } from "./Button";

type Theme = "light" | "dark";

function currentTheme(): Theme {
  const chosen = document.documentElement.dataset.theme;
  if (chosen === "light" || chosen === "dark") {
    return chosen;
  }
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

type ThemeToggleProps = { toLightLabel: string; toDarkLabel: string };

export function ThemeToggle({ toLightLabel, toDarkLabel }: ThemeToggleProps) {
  const [theme, setTheme] = useState<Theme | null>(null);

  // The saved theme exists only in the browser, so it is read after mounting.
  useEffect(() => setTheme(currentTheme()), []);

  function toggle() {
    const next = theme === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = next;
    setTheme(next);
    try {
      localStorage.setItem("theme", next);
    } catch {
      // Storage can be blocked. The theme still applies until the page closes.
    }
  }

  return (
    <Button variant="quiet" onClick={toggle} disabled={theme === null}>
      {theme === "dark" ? toLightLabel : toDarkLabel}
    </Button>
  );
}
