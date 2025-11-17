import clsx from 'clsx';
import { FieldValues } from 'react-hook-form';
import { FormProps } from '@/components/ui/form/TextFormField.tsx';

interface DateTimeInputProps<T extends FieldValues> {
  title: string;
  formHook?: FormProps<T>;
  disabled?: boolean;
  testId?: string;
}

export const DateTimeInput = <T extends FieldValues>({
  title,
  formHook,
  disabled = false,
  testId,
}: DateTimeInputProps<T>) => {
  const fieldError = formHook?.errors?.[formHook?.name]?.message?.toString();
  const baseClasses =
    'h-10 rounded-radius-md border border-solid w-full bg-color-surface-primary';
  const textColorClasses = disabled
    ? 'text-color-text-disabled'
    : 'text-color-text-secondary';
  const paddingClasses = 'px-3 py-2';
  const borderColorClasses = disabled
    ? 'border-color-border-primary'
    : formHook?.errors?.[formHook.name as keyof T]
      ? 'border-color-status-error-dark'
      : 'border-color-border-primary';
  const backgroundClasses = disabled
    ? 'cursor-not-allowed'
    : 'hover:bg-color-brand-light hover:border-color-brand-dark focus:border-color-border-primary';

  return (
    <div className="flex flex-col items-start self-stretch gap-1">
      <span className="text-caption-2">{title}</span>
      <div
        className={clsx(
          'relative flex items-center text-body-1 w-full',
          textColorClasses
        )}
      >
        <input
          aria-label="Date and time"
          type="datetime-local"
          data-testid={testId}
          className={clsx(
            baseClasses,
            paddingClasses,
            borderColorClasses,
            backgroundClasses,
            textColorClasses
          )}
          {...(formHook?.register && formHook.name
            ? formHook.register(formHook.name, {
                ...formHook.rules,
                setValueAs: (value: string) =>
                  value === '' ? undefined : value,
              })
            : {})}
        />
      </div>
      {fieldError && (
        <span className="text-caption-1 text-color-status-error-dark">
          {fieldError}
        </span>
      )}
    </div>
  );
};
