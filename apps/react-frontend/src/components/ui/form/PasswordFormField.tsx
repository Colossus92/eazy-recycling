import {
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';
import { PasswordInput } from './PasswordInput';

export interface FormProps<TFieldValues extends FieldValues> {
  register: UseFormRegister<TFieldValues>;
  name: Path<TFieldValues>;
  rules?: RegisterOptions<TFieldValues>;
  errors?: FieldErrors<TFieldValues>;
}

interface PasswordFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder?: string;
  value?: string;
  formHook?: FormProps<TFieldValues>;
  disabled?: boolean;
}

export const PasswordFormField = <TFieldValues extends FieldValues>({
  title,
  placeholder,
  formHook,
  value,
  disabled,
}: PasswordFormFieldProps<TFieldValues>) => {
  const fieldError = formHook?.errors?.[formHook?.name]?.message?.toString();

  return (
    <div className="flex flex-col items-start self-stretch gap-1">
      <span className="text-subtitle-1">{title}</span>
      <PasswordInput
        placeholder={placeholder}
        formHook={{
          register: formHook?.register,
          name: formHook?.name,
          rules: formHook?.rules,
          errors: formHook?.errors,
        }}
        defaultValue={value}
        disabled={disabled}
        data-test-id={formHook?.name}
      />
      {fieldError && (
        <span className="text-caption-1 text-color-status-error-dark">
          {fieldError}
        </span>
      )}
    </div>
  );
};
