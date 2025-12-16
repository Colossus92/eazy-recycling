import { InputHTMLAttributes, useEffect, useRef } from 'react';
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

/**
 * Calculate decimal places from step value
 */
const getDecimalPlaces = (step: number | string): number => {
  if (step === 'any') return 2;
  const stepNum = typeof step === 'string' ? parseFloat(step) : step;
  if (isNaN(stepNum) || stepNum <= 0) return 2;
  return stepNum >= 1 ? 0 : Math.max(0, Math.round(-Math.log10(stepNum)));
};

/**
 * Format a number value according to the step precision
 */
const formatValue = (value: string | number | undefined, step: number | string): string => {
  const parsed = parseNumber(value);
  if (isNaN(parsed)) return '';
  return parsed.toFixed(getDecimalPlaces(step));
};

export const NumberInput = <TFieldValues extends FieldValues>({
  disabled = false,
  placeholder,
  formHook,
  step = 'any',
  onBlur: propsOnBlur,
  defaultValue,
  ...props
}: InputProps<TFieldValues>) => {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const fieldError = getFieldError(formHook?.errors, formHook?.name);

  // Get register result to extract the ref
  const registerResult = formHook?.register && formHook?.name
    ? formHook.register(formHook.name, {
        ...(formHook?.rules || {}),
        setValueAs: (v: string) => {
          if (v === '' || v === null || v === undefined) return undefined;
          const parsed = parseNumber(v);
          return isNaN(parsed) ? undefined : parsed;
        }
      } as any)
    : null;

  // Merge refs: our local ref + react-hook-form's ref
  const setRef = (element: HTMLInputElement | null) => {
    inputRef.current = element;
    if (registerResult?.ref) {
      registerResult.ref(element);
    }
  };

  // Format the input value on initial render and when defaultValue changes
  useEffect(() => {
    if (inputRef.current && inputRef.current.value) {
      inputRef.current.value = formatValue(inputRef.current.value, step);
    }
  }, [defaultValue, step]);

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
    const formatted = formatValue(e.target.value, step);
    if (formatted) {
      e.target.value = formatted;
    }
    propsOnBlur?.(e);
  };

  return (
    <div className={clsx(formInputClasses.container, textColorClasses)}>
      <input
        ref={setRef}
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
        defaultValue={defaultValue}
        name={registerResult?.name}
        onChange={registerResult?.onChange}
        {...props}
        onBlur={handleBlur}
      />
    </div>
  );
};
