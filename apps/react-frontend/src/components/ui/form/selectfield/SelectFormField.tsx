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
import { RequiredMarker } from '../RequiredMarker';

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
  testId?: string;
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
  testId,
  formHook,
}: SelectFormFieldProps<TFieldValues>) => {
  const { name, rules, errors, control } = formHook;
  const error = errors[name]?.message as string;

  return (
    <div className="flex flex-col items-start self-stretch gap-1 w-full">
      <div className="flex items-center self-stretch justify-between">
        <span className="text-caption-2">
          {title}
          <RequiredMarker required={rules?.required} />
        </span>
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
            id={testId || `select-${name}`}
            menuPortalTarget={document.body}
            className={clsx(
              'w-full text-body-1',
              disabled
                ? 'text-color-text-disabled cursor-not-allowed'
                : 'text-color-text-secondary'
            )}
            styles={{
              control: (base, state) => ({
                ...base,
                minHeight: '40px',
                height: '40px',
                borderRadius: '8px',
                borderWidth: '1px',
                borderStyle: 'solid',
                borderColor: disabled
                  ? '#E3E8F3'
                  : error
                    ? '#F04438'
                    : state.isFocused
                      ? '#1E77F8'
                      : '#E3E8F3',
                backgroundColor: '#FFFFFF',
                cursor: disabled ? 'not-allowed' : 'default',
                boxShadow: 'none',
                '&:hover': {
                  borderColor: disabled
                    ? '#E3E8F3'
                    : error
                      ? '#F04438'
                      : '#1E77F8',
                  backgroundColor: disabled ? '#FFFFFF' : '#F3F8FF',
                },
              }),
              menuPortal: (base) => ({
                ...base,
                zIndex: 9999,
              }),
              input: (base) => ({
                ...base,
                'input:focus': {
                  boxShadow: 'none',
                },
              }),
            }}
            classNames={{
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
