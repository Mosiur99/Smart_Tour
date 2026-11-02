import { useId, type InputHTMLAttributes } from "react";

type TextFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, "id"> & {
  label: string;
  hint?: string;
  error?: string;
};

export function TextField({ label, hint, error, className = "", ...rest }: TextFieldProps) {
  const id = useId();
  const messageId = `${id}-message`;
  const message = error ?? hint;

  return (
    <div className={`flex flex-col gap-1 ${className}`}>
      <label htmlFor={id} className="font-semibold">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={message ? messageId : undefined}
        className={`min-h-12 w-full rounded-control border bg-surface px-3.5 text-ink ${
          error ? "border-problem" : "border-stitch"
        }`}
        {...rest}
      />
      {message && (
        <span id={messageId} className={`text-sm ${error ? "text-problem" : "text-muted"}`}>
          {message}
        </span>
      )}
    </div>
  );
}
