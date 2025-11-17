import clsx from 'clsx';
import { FieldValues, FieldErrors, Path, RegisterOptions, UseFormRegister } from 'react-hook-form';
import { formInputClasses } from '@/styles/formInputClasses.ts';
import { getFieldError } from '@/utils/formErrorUtils';
import CalendarDots from '@/assets/icons/CalendarDots.svg?react';

interface DateFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder: string;
  disabled?: boolean;
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    rules?: RegisterOptions<TFieldValues>;
    errors: FieldErrors;
  };
  testId?: string;
}

export const DateFormField = <TFieldValues extends FieldValues>({
  title,
  placeholder,
  disabled = false,
  formHook,
  testId,
}: DateFormFieldProps<TFieldValues>) => {
  const { register, name, rules } = formHook;
  const fieldError = getFieldError(formHook?.errors, formHook?.name);
  const textColorClasses = disabled
    ? formInputClasses.text.disabled
    : formInputClasses.text.default;
  const borderColorClasses = disabled
    ? formInputClasses.border.disabled
    : fieldError
      ? formInputClasses.border.error
      : formInputClasses.border.default;
  const backgroundClasses = disabled
    ? formInputClasses.background.disabled
    : formInputClasses.background.hover;

  return (
    <div className="flex flex-col items-start self-stretch gap-1 w-full">
      <div className="flex items-center self-stretch justify-between">
        <span className="text-subtitle-2 text-color-text-primary">{title}</span>
      </div>
      <div className={clsx(
        'relative flex items-center w-full',
        formInputClasses.base,
        formInputClasses.padding.default,
        textColorClasses,
        backgroundClasses,
        borderColorClasses
      )}>
        <CalendarDots className="h-5 w-5 text-color-text-secondary mr-2" />
        <input
          type="date"
          className="flex-1 bg-transparent border-none outline-none text-body-1"
          placeholder={placeholder}
          disabled={disabled}
          data-testid={testId}
          {...register(name, rules)}
        />
      </div>
      {fieldError && <span className="text-caption-1 text-color-status-error-dark">
        {fieldError}
      </span>}
    </div>
  );
};
