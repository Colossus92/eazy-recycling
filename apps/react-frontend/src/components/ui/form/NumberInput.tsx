import { InputHTMLAttributes } from 'react';
import clsx from 'clsx';
import { FieldValues } from 'react-hook-form';
import { FormProps } from './TextFormField.tsx';
import { formInputClasses } from '@/styles/formInputClasses.ts';
import { getFieldError } from '@/utils/formErrorUtils';

interface InputProps<TFieldValues extends FieldValues>
  extends InputHTMLAttributes<HTMLInputElement> {
  disabled?: boolean;
  placeholder?: string;
  formHook?: FormProps<TFieldValues>;
  step?: number | string;
}

export const NumberInput = <TFieldValues extends FieldValues>({
  disabled = false,
  placeholder,
  formHook,
  step = 'any',
  onBlur: propsOnBlur,
  ...props
}: InputProps<TFieldValues>) => {
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

  const handleBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    const value = e.target.value;
    if (value && !isNaN(parseFloat(value))) {
      e.target.value = parseFloat(value).toFixed(step === 'any' ? 1 : step as number);
    }
    propsOnBlur?.(e);
  };

  return (
    <div className={clsx(formInputClasses.container, textColorClasses)}>
      <input
        type={'number'}
        step={step}
        placeholder={placeholder}
        className={clsx(
          'h-10',
          'text-right',
          formInputClasses.base,
          formInputClasses.padding.default,
          borderColorClasses,
          backgroundClasses,
          textColorClasses
        )}
        disabled={disabled}
        {...(formHook?.register && formHook?.name
          ? formHook?.register(formHook?.name, { 
              ...(formHook?.rules || {}), 
              setValueAs: (v: string) => {
                if (v === '' || v === null || v === undefined) return undefined;
                const parsed = parseFloat(v);
                return isNaN(parsed) ? undefined : parsed;
              }
            } as any)
          : {})}
        {...props}
        onBlur={handleBlur}
      />
    </div>
  );
};
