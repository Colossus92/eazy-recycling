import { FieldErrors, FieldValues, Path, Control, RegisterOptions } from 'react-hook-form';
import { Controller } from 'react-hook-form';
import { RadioGroup, Label, Field, Radio } from '@headlessui/react';

export interface RadioOption {
  value: string;
  label: string;
}

export interface RadioFormFieldProps<T extends FieldValues> {
  title?: string;
  options: RadioOption[];
  formHook: {
    name: Path<T>;
    rules?: RegisterOptions<T>;
    errors: FieldErrors<T>;
    control?: Control<T>;
  };
  disabled?: boolean;
  testId?: string;
}

export const RadioFormField = <T extends FieldValues>({
  title,
  options,
  formHook,
  disabled = false,
  testId,
}: RadioFormFieldProps<T>) => {
  const { name, rules, errors, control } = formHook;
  const error = errors[name];

  return (
    <div className="space-y-2 w-full">
      {title && <label className="text-caption-2">{title}</label>}
      <Controller
        control={control}
        name={name}
        rules={rules}
        render={({ field }) => (
          <RadioGroup
            value={field.value}
            onChange={field.onChange}
            disabled={disabled}
            className="w-full"
          >
            <div className="flex flex-row items-center gap-4">
              {options.map((option) => (
                <Field
                  key={option.value}
                  className={`flex items-center gap-2 ${disabled ? 'cursor-not-allowed' : 'cursor-pointer'}`}
                >
                  <Radio
                    value={option.value}
                    disabled={disabled}
                    data-testid={testId ? `${testId}-${option.value}` : undefined}
                    className={`h-5 w-5 rounded-full border border-gray-300 bg-white ${disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'}`}
                  >
                    {({ checked }) => (
                      <div className="flex items-center justify-center h-full w-full">
                        {checked && (
                          <div className="h-2.5 w-2.5 rounded-full bg-blue-600" />
                        )}
                      </div>
                    )}
                  </Radio>
                  <Label className={`text-sm ${disabled ? 'cursor-not-allowed text-color-text-disabled' : 'cursor-pointer text-color-text-primary'}`}>
                    {option.label}
                  </Label>
                </Field>
              ))}
            </div>
          </RadioGroup>
        )}
      />
      {error && (
        <span className="text-caption-2 text-color-text-error">
          {error.message as string}
        </span>
      )}
    </div>
  );
};
