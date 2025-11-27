import { TextFormField } from './TextFormField';
import {
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';

interface PhoneNumberFormFieldProps<T extends FieldValues> {
  formHook?: FormProps<T>;
  value?: string;
  title?: string;
  placeholder?: string;
  disabled?: boolean;
}

export interface FormProps<TFieldValues extends FieldValues> {
  name?: Path<TFieldValues>;
  register?: UseFormRegister<TFieldValues>;
  rules?: RegisterOptions<TFieldValues>;
  errors?: FieldErrors<TFieldValues>;
}

export const PhoneNumberFormField = <T extends FieldValues>({
  formHook,
  disabled = false,
  value,
  title = 'Telefoonnummer',
  placeholder = 'Vul telefoonnummer in',
}: PhoneNumberFormFieldProps<T>) => {
  return (
    <TextFormField
      title={title}
      placeholder={placeholder}
      formHook={formHook}
      value={value}
      disabled={disabled}
    />
  );
};
