import { InputHTMLAttributes } from 'react';
import clsx from 'clsx';
import { FieldValues } from 'react-hook-form';
import { FormProps } from './TextFormField.tsx';
import { formInputClasses } from '@/styles/formInputClasses.ts';

interface InputProps<TFieldValues extends FieldValues>
  extends InputHTMLAttributes<HTMLInputElement> {
  disabled?: boolean;
  placeholder?: string;
  formHook?: FormProps<TFieldValues>;
  step?: number | 'any';
}

export const NumberInput = <TFieldValues extends FieldValues>({
  disabled = false,
  placeholder,
  formHook,
  ...props
}: InputProps<TFieldValues>) => {
  const textColorClasses = disabled
    ? formInputClasses.text.disabled
    : formInputClasses.text.default;
  const borderColorClasses = disabled
    ? formInputClasses.border.disabled
    : formHook?.errors?.[formHook.name as keyof TFieldValues]
      ? formInputClasses.border.error
      : formInputClasses.border.default;
  const backgroundClasses = disabled
    ? formInputClasses.background.disabled
    : formInputClasses.background.hover;

  return (
    <div className={clsx(formInputClasses.container, textColorClasses)}>
      <input
        type={'number'}
        step={'any'}
        placeholder={placeholder}
        className={clsx(
          'h-10',
          formInputClasses.base,
          formInputClasses.padding.default,
          borderColorClasses,
          backgroundClasses,
          textColorClasses
        )}
        disabled={disabled}
        {...(formHook?.register && formHook?.name
          ? formHook?.register(formHook?.name, formHook?.rules)
          : {})}
        {...props}
      />
    </div>
  );
};
