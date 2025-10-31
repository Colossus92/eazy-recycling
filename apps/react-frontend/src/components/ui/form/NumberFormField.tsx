import {
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';
import { NumberInput } from '@/components/ui/form/NumberInput.tsx';

interface TextFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder?: string;
  formHook?: FormProps<TFieldValues>;
  disabled?: boolean;
  step: number | 'any';
}

export interface FormProps<TFieldValues extends FieldValues> {
  name?: Path<TFieldValues>;
  register?: UseFormRegister<TFieldValues>;
  rules?: RegisterOptions<TFieldValues>;
  errors?: FieldErrors<TFieldValues>;
}

export const NumberFormField = <TFieldValues extends FieldValues>({
  title,
  placeholder,
  formHook,
  disabled,
  step,
}: TextFormFieldProps<TFieldValues>) => {
  const fieldError = formHook?.errors?.[formHook?.name]?.message?.toString();
  return (
    <div className="flex flex-col items-start self-stretch gap-1">
      <span className="text-subtitle-2">{title}</span>
      <NumberInput
        placeholder={placeholder}
        formHook={{
          register: formHook?.register,
          name: formHook?.name,
          rules: formHook?.rules,
          errors: formHook?.errors,
        }}
        step={step}
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
