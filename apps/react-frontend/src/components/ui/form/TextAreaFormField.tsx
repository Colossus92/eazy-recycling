import {
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';
import clsx from 'clsx';
import { formInputClasses } from '@/styles/formInputClasses.ts';

interface TextAreaFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder: string;
  value?: string;
  disabled?: boolean;
  rows?: number;
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    rules?: RegisterOptions<TFieldValues>;
    errors: FieldErrors;
  };
  testId?: string;
}

export const TextAreaFormField = <TFieldValues extends FieldValues>({
  title,
  placeholder,
  value,
  disabled = false,
  rows = 5,
  formHook,
  testId,
}: TextAreaFormFieldProps<TFieldValues>) => {
  const { register, name, rules, errors } = formHook;
  const error = errors[name]?.message as string;
  const borderColorClasses = error
    ? formInputClasses.border.error
    : formInputClasses.border.default;

  return (
    <div className="flex flex-col items-start self-stretch gap-1 w-full">
      <div className="flex items-center self-stretch justify-between">
        <span className="text-subtitle-2">{title}</span>
      </div>
      <textarea
        className={clsx(
          formInputClasses.base,
          formInputClasses.padding.default,
          formInputClasses.text.default,
          formInputClasses.background.hover,
          borderColorClasses
        )}
        placeholder={placeholder}
        defaultValue={value}
        disabled={disabled}
        rows={rows}
        data-testid={testId}
        {...register(name, rules)}
      />
      {error && <span className="text-caption-1 text-color-status-error-dark">
        {error}
      </span>}
    </div>
  );
};
