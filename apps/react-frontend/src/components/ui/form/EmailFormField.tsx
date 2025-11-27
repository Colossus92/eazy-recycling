import { TextFormField } from './TextFormField';
import { FieldErrors, FieldValues, Path, UseFormRegister } from 'react-hook-form';

interface EmailFormFieldProps<T extends FieldValues> {
  register: UseFormRegister<T>;
  errors: FieldErrors<T>;
  name: Path<T>;
  value?: string;
  title?: string;
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const EmailFormField = <T extends FieldValues>({
  register,
  errors,
  name,
  value,
  title = 'Email',
  placeholder = 'Vul email in',
  disabled = false,
  required = false,
}: EmailFormFieldProps<T>) => {
  return (
    <TextFormField
      title={title}
      placeholder={placeholder}
      formHook={{
        register,
        name,
        errors,
        rules: {
          required: required ? 'Email is verplicht' : undefined,
          pattern: {
            value: EMAIL_PATTERN,
            message: 'Voer een geldig e-mailadres in',
          },
        },
      }}
      value={value}
      disabled={disabled}
    />
  );
};
