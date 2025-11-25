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

/**
 * Helper function to parse number strings that may use comma or period as decimal separator
 * @param value - string or number value to parse
 * @returns parsed number or NaN if invalid
 */
const parseNumber = (value: string | number | undefined): number => {
  if (value === undefined || value === null || value === '') return NaN;
  if (typeof value === 'number') return value;
  // Replace comma with period to handle both decimal separators
  const normalizedValue = String(value).replace(',', '.');
  return parseFloat(normalizedValue);
};

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
    const parsed = parseNumber(value);
    if (!isNaN(parsed)) {
      // Determine decimal places based on step value
      // step = 1 → 0 decimals, step = 0.1 → 1 decimal, step = 0.01 → 2 decimals
      let decimalPlaces = 2; // default for 'any'
      if (step !== 'any') {
        const stepNum = typeof step === 'string' ? parseFloat(step) : step;
        if (!isNaN(stepNum) && stepNum > 0) {
          // Calculate decimal places from step: -log10(step) gives us the number of decimals
          // For step=1: -log10(1) = 0, step=0.1: -log10(0.1) = 1, step=0.01: -log10(0.01) = 2
          decimalPlaces = stepNum >= 1 ? 0 : Math.max(0, Math.round(-Math.log10(stepNum)));
        }
      }
      e.target.value = parsed.toFixed(decimalPlaces);
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
                const parsed = parseNumber(v);
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
