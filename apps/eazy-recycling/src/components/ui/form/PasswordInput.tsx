import {
  FunctionComponent,
  InputHTMLAttributes,
  SVGProps,
  useState,
} from 'react';
import clsx from 'clsx';
import { FieldValues } from 'react-hook-form';
import { FormProps } from './TextFormField.tsx';
import EyeSlashSolid from '@/assets/icons/EyeSlashSolid.svg?react';
import EyeSolid from '@/assets/icons/EyeSolid.svg?react';

interface PasswordInputProps<TFieldValues extends FieldValues>
  extends InputHTMLAttributes<HTMLInputElement> {
  icon?: FunctionComponent<SVGProps<SVGSVGElement>>;
  disabled?: boolean;
  placeholder?: string;
  formHook?: FormProps<TFieldValues>;
}

export const PasswordInput = <TFieldValues extends FieldValues>({
  icon: Icon,
  disabled = false,
  placeholder,
  formHook,
  ...props
}: PasswordInputProps<TFieldValues>) => {
  const [showPassword, setShowPassword] = useState(false);

  // Base styling classes
  const baseClasses =
    'h-10 rounded-radius-md border border-solid w-full bg-color-surface-primary';
  const textColorClasses = disabled
    ? 'text-color-text-disabled'
    : 'text-color-text-secondary';
  const paddingClasses = Icon ? 'pl-10 pr-10' : 'pl-3 pr-10 py-2'; // Always leave space for the toggle button
  const borderColorClasses = disabled
    ? 'border-color-border-primary'
    : formHook?.errors?.[formHook.name as keyof TFieldValues]
      ? 'border-color-status-error-dark'
      : 'border-color-border-primary';
  const backgroundClasses = disabled
    ? 'cursor-not-allowed'
    : 'hover:bg-color-brand-light hover:border-color-brand-dark focus:border-color-border-primary';

  const togglePassword = () => {
    setShowPassword(!showPassword);
  };

  return (
    <div
      className={clsx(
        'relative flex items-center text-body-1 w-full',
        textColorClasses
      )}
    >
      {Icon && (
        <div className="absolute left-3 pointer-events-none flex items-center">
          <Icon />
        </div>
      )}
      <input
        type={showPassword ? 'text' : 'password'}
        placeholder={placeholder}
        className={clsx(
          baseClasses,
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
      <button
        type="button"
        className="absolute right-3 flex items-center justify-center text-color-text-secondary hover:text-color-text-primary focus:outline-none"
        onClick={togglePassword}
        tabIndex={-1} // Don't include in tab order
        disabled={disabled}
      >
        {showPassword ? (
          <EyeSolid className={'w-5 h-5'} />
        ) : (
          <EyeSlashSolid className={'w-5 h-5'} />
        )}
      </button>
    </div>
  );
};
