import {
  FieldErrors,
  FieldValues,
  Path,
  PathValue,
  UseFormRegister,
  UseFormSetValue,
} from 'react-hook-form';
import { TextFormField } from '@/components/ui/form/TextFormField.tsx';

interface PostalCodeFormFieldProps<T extends FieldValues> {
  register: UseFormRegister<T>;
  setValue: UseFormSetValue<T>;
  errors: FieldErrors<T>;
  name: Path<T>;
  value?: string;
  required?: boolean;
  disabled?: boolean;
}

export const PostalCodeFormField = <T extends FieldValues>({
  register,
  setValue,
  errors,
  name,
  value,
  required = true,
  disabled = false,
}: PostalCodeFormFieldProps<T>) => {
  const formatPostalCode = (value: string) => {
    const match = value.trim().match(/^(\d{4})\s?([A-Za-z]{2})$/);
    return match ? `${match[1]} ${match[2].toUpperCase()}` : value;
  };

  return (
    <TextFormField
      title={'Postcode'}
      placeholder={'Vul postcode in'}
      formHook={{
        register: (name, rules) =>
          register(name, {
            ...rules,
            onBlur: (e: { target: { value: string } }) => {
              const formatted = formatPostalCode(e.target.value);
              setValue(name, formatted as PathValue<T, Path<T>>);
            },
          }),
        name: name,
        rules: {
          required: required ? 'Postcode is verplicht' : undefined,
          pattern: {
            value: /^\d{4}\s[A-Z]{2}$/,
            message: 'Postcode moet het formaat 1234 AB hebben',
          },
        },
        errors,
      }}
      value={value}
      disabled={disabled}
    />
  );
};
