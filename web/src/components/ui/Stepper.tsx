"use client";

type StepperProps = {
  label: string;
  value: number;
  /** The value as read aloud and shown, such as "3 days" or "৩ দিন". */
  valueText: string;
  min: number;
  max: number;
  onChange: (value: number) => void;
  decreaseLabel: string;
  increaseLabel: string;
};

const STEP_BUTTON =
  "size-11 cursor-pointer rounded-control border border-stitch bg-surface text-xl text-ink hover:bg-sunk disabled:cursor-not-allowed disabled:opacity-40";

export function Stepper({
  label,
  value,
  valueText,
  min,
  max,
  onChange,
  decreaseLabel,
  increaseLabel,
}: StepperProps) {
  return (
    <div role="group" aria-label={label} className="flex flex-col gap-1">
      <span aria-hidden="true" className="font-semibold">
        {label}
      </span>
      <div className="flex items-center justify-between gap-2">
        <button
          type="button"
          aria-label={decreaseLabel}
          disabled={value <= min}
          onClick={() => onChange(value - 1)}
          className={STEP_BUTTON}
        >
          −
        </button>
        <output aria-live="polite" className="min-w-16 text-center font-semibold">
          {valueText}
        </output>
        <button
          type="button"
          aria-label={increaseLabel}
          disabled={value >= max}
          onClick={() => onChange(value + 1)}
          className={STEP_BUTTON}
        >
          +
        </button>
      </div>
    </div>
  );
}
