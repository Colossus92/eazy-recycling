import {
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';
import { NumberInput } from '@/components/ui/form/NumberInput.tsx';
import { getFieldError } from '@/utils/formErrorUtils';

interface TextFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder?: string;
  value?: string;
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
  value,
  disabled,
  step,
}: TextFormFieldProps<TFieldValues>) => {
  const fieldError = getFieldError(formHook?.errors, formHook?.name);
  return (
    <div className="flex flex-col items-start self-stretch gap-1">
      <span className="text-caption-2">{title}</span>
      <NumberInput
        placeholder={placeholder}
        formHook={{
          register: formHook?.register,
          name: formHook?.name,
          rules: formHook?.rules,
          errors: formHook?.errors,
        }}
        step={step}
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
