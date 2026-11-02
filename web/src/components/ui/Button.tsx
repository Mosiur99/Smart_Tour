import type { ButtonHTMLAttributes } from "react";

type Variant = "primary" | "secondary" | "quiet";

const VARIANT_CLASSES: Record<Variant, string> = {
  primary: "bg-accent text-on-accent border-accent hover:bg-accent-hover hover:border-accent-hover",
  secondary: "bg-transparent text-ink border-stitch hover:bg-sunk",
  quiet: "bg-transparent text-accent border-transparent hover:bg-sunk",
};

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  /** Keeps the button in place and blocks a second submit while work runs. */
  busy?: boolean;
};

export function Button({
  variant = "primary",
  busy = false,
  disabled,
  type = "button",
  className = "",
  children,
  ...rest
}: ButtonProps) {
  return (
    <button
      type={type}
      disabled={disabled || busy}
      aria-busy={busy || undefined}
      className={`min-h-12 cursor-pointer rounded-control border px-5 font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${VARIANT_CLASSES[variant]} ${className}`}
      {...rest}
    >
      {children}
    </button>
  );
}
