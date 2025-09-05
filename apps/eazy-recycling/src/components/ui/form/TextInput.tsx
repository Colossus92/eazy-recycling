import { FunctionComponent, InputHTMLAttributes, SVGProps } from 'react';
import clsx from 'clsx';
import { FieldValues } from 'react-hook-form';
import { FormProps } from './TextFormField.tsx';
import { formInputClasses } from '@/styles/formInputClasses.ts';

interface InputProps<TFieldValues extends FieldValues>
  extends InputHTMLAttributes<HTMLInputElement> {
  icon?: FunctionComponent<SVGProps<SVGSVGElement>>;
  disabled?: boolean;
  placeholder?: string;
  formHook?: FormProps<TFieldValues>;
}

export const TextInput = <TFieldValues extends FieldValues>({
  icon: Icon,
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
  const paddingClasses = Icon
    ? formInputClasses.padding.withIcon
    : formInputClasses.padding.default;

  return (
    <div className={clsx(formInputClasses.container, textColorClasses)}>
      {Icon && (
        <div className={formInputClasses.icon}>
          <Icon />
        </div>
      )}
      <input
        type={'text'}
        placeholder={placeholder}
        className={clsx(
          'h-10',
          formInputClasses.base,
          paddingClasses,
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
