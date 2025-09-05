import {
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';
import { TextInput } from './TextInput.tsx';

interface TextFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder?: string;
  value?: string;
  formHook?: FormProps<TFieldValues>;
  disabled?: boolean;
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
}: TextFormFieldProps<TFieldValues>) => {
  const fieldError = formHook?.errors?.[formHook?.name]?.message?.toString();
  return (
    <div className="flex flex-1 flex-col items-start self-stretch gap-1">
      <span className="text-subtitle-2">{title}</span>
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
      />
      {fieldError && (
        <span className="text-caption-1 text-color-status-error-dark">
          {fieldError}
        </span>
      )}
    </div>
  );
};
