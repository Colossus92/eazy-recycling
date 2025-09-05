import {
  Control,
  FieldErrors,
  FieldPath,
  FieldPathValue,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';
import Select from 'react-select';
import { Controller } from 'react-hook-form';
import clsx from 'clsx';

export interface Option {
  value: string;
  label: string;
}

interface SelectFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder: string;
  options: Option[];
  value?: FieldPathValue<TFieldValues, FieldPath<TFieldValues>>;
  disabled?: boolean;
  isMulti?: boolean;
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    rules?: RegisterOptions<TFieldValues>;
    errors: FieldErrors;
    control?: Control<TFieldValues>;
  };
}

export const SelectFormField = <TFieldValues extends FieldValues>({
  title,
  placeholder,
  options,
  value,
  disabled = false,
  isMulti = false,
  formHook,
}: SelectFormFieldProps<TFieldValues>) => {
  const { name, rules, errors, control } = formHook;
  const error = errors[name]?.message as string;

  return (
    <div className="flex flex-col items-start self-stretch gap-1 w-full">
      <div className="flex items-center self-stretch justify-between">
        <span className="text-caption-2">{title}</span>
      </div>

      <Controller
        control={control}
        name={name}
        rules={rules}
        defaultValue={value}
        render={({ field }) => (
          <Select
            {...field}
            options={options}
            placeholder={placeholder}
            isDisabled={disabled}
            isMulti={isMulti}
            isClearable={true}
            classNamePrefix="react-select"
            noOptionsMessage={() => 'Geen opties beschikbaar'}
            className={clsx(
              'w-full text-body-1',
              disabled
                ? 'text-color-text-disabled'
                : 'text-color-text-secondary'
            )}
            classNames={{
              control: ({ isFocused }) =>
                clsx(
                  'h-10 rounded-radius-md border border-solid w-full bg-color-surface-primary',
                  disabled
                    ? 'border-color-border-primary cursor-not-allowed'
                    : error
                      ? 'border-color-status-error-dark'
                      : clsx(
                          'border-color-border-primary',
                          isFocused ? 'border-color-primary' : '',
                          !isFocused &&
                            'hover:bg-color-brand-light hover:border-color-brand-dark'
                        )
                ),
              placeholder: () => clsx('text-color-text-disabled', 'italic'),
              option: ({ isSelected, isFocused }) =>
                clsx(
                  'cursor-pointer',
                  isSelected
                    ? 'bg-color-primary text-color-text-secondary'
                    : isFocused
                      ? 'bg-color-surface-secondary text-color-text-primary'
                      : 'bg-color-surface-primary text-color-text-primary'
                ),
              singleValue: () => 'text-color-text-primary',
              multiValue: () =>
                'bg-color-surface-secondary text-color-text-primary rounded-sm',
              valueContainer: () => 'px-3 py-2',
            }}
            value={
              isMulti
                ? field.value
                  ? options.filter((option) =>
                      Array.isArray(field.value)
                        ? field.value.includes(option.value)
                        : option.value === field.value
                    )
                  : []
                : options.find((option) => option.value === field.value) || null
            }
            onChange={(selectedOption) => {
              if (isMulti) {
                const values = selectedOption
                  ? (selectedOption as Option[]).map((option) => option.value)
                  : [];
                field.onChange(values);
              } else {
                field.onChange(
                  selectedOption ? (selectedOption as Option).value : null
                );
              }
            }}
          />
        )}
      />
      {error && (
        <span className="text-caption-1 text-color-status-error-dark">
          {error}
        </span>
      )}
    </div>
  );
};
