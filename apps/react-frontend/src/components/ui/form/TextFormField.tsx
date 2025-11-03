import {
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';
import { TextInput } from './TextInput.tsx';
import { getFieldError } from '@/utils/formErrorUtils';

interface TextFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder?: string;
  value?: string;
  formHook?: FormProps<TFieldValues>;
  disabled?: boolean;
  testId?: string;
}

export interface FormProps<TFieldValues extends FieldValues> {
  name?: Path<TFieldValues>;
  register?: UseFormRegister<TFieldValues>;
  rules?: RegisterOptions<TFieldValues>;
  errors?: FieldErrors<TFieldValues>;
}

export const TextFormField = <TFieldValues extends FieldValues>({
  title,
  placeholder,
  formHook,
  value,
  disabled,
  testId,
}: TextFormFieldProps<TFieldValues>) => {
  const fieldError = getFieldError(formHook?.errors, formHook?.name);
  return (
    <div className="flex flex-1 flex-col items-start self-stretch gap-1">
      <span className="text-caption-2">{title}</span>
      <TextInput
        placeholder={placeholder}
        formHook={{
          register: formHook?.register,
          name: formHook?.name,
          rules: formHook?.rules,
          errors: formHook?.errors,
        }}
        defaultValue={value}
        disabled={disabled}
        data-testid={testId || formHook?.name}
      />
      {fieldError && (
        <span className="text-caption-1 text-color-status-error-dark">
          {fieldError}
        </span>
      )}
    </div>
  );
};
